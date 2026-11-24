package com.parcelplus.shipment.api;

import com.jayway.jsonpath.JsonPath;
import com.parcelplus.shipment.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Status-change endpoint and the problem+json mapping of each error, against real Postgres. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ShipmentStatusChangeIT {

    private static final String PROBLEM = "application/problem+json";

    @Autowired
    MockMvc mvc;

    private String createShipment() throws Exception {
        String json = mvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originHub":"HUB-001","destinationHub":"HUB-002","serviceLevel":"STANDARD"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private ResultActions changeStatus(String id, String json) throws Exception {
        return mvc.perform(post("/api/shipments/" + id + "/status-changes")
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String statusBody(String status, String hubCode) {
        return """
                {"status":%s,"hubCode":%s}""".formatted(
                status == null ? "null" : "\"" + status + "\"",
                hubCode == null ? "null" : "\"" + hubCode + "\"");
    }

    @Test
    void validTransitionIsAppliedAndPersisted() throws Exception {
        String id = createShipment();

        changeStatus(id, statusBody("PICKED_UP", "HUB-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"))
                .andExpect(jsonPath("$.lastSequence").value(1));

        mvc.perform(get("/api/shipments/" + id))
                .andExpect(jsonPath("$.status").value("PICKED_UP"))
                .andExpect(jsonPath("$.lastSequence").value(1));
    }

    @Test
    void illegalTransitionReturns409WithStatusProperties() throws Exception {
        String id = createShipment();

        changeStatus(id, statusBody("DELIVERED", "HUB-001"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Invalid status transition"))
                .andExpect(jsonPath("$.shipmentId").value(id))
                .andExpect(jsonPath("$.currentStatus").value("CREATED"))
                .andExpect(jsonPath("$.requestedStatus").value("DELIVERED"));
    }

    @Test
    void illegalTransitionLeavesShipmentUnchanged() throws Exception {
        String id = createShipment();

        changeStatus(id, statusBody("DELIVERED", "HUB-001")).andExpect(status().isConflict());

        mvc.perform(get("/api/shipments/" + id))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.lastSequence").value(0));
    }

    @Test
    void unknownShipmentReturns404Problem() throws Exception {
        changeStatus(UUID.randomUUID().toString(), statusBody("PICKED_UP", "HUB-001"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Shipment not found"));
    }

    @Test
    void getUnknownShipmentReturns404Problem() throws Exception {
        mvc.perform(get("/api/shipments/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }

    @Test
    void badHubCodeReturns400WithFieldErrors() throws Exception {
        changeStatus(createShipment(), statusBody("PICKED_UP", "PARIS"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("hubCode"));
    }

    @Test
    void missingStatusReturns400WithFieldErrors() throws Exception {
        changeStatus(createShipment(), statusBody(null, "HUB-001"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    @Test
    void unknownStatusValueReturns400Problem() throws Exception {
        changeStatus(createShipment(), statusBody("TELEPORTED", "HUB-001"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }

    @Test
    void idThatIsNotAUuidReturns400Problem() throws Exception {
        changeStatus("not-a-uuid", statusBody("PICKED_UP", "HUB-001"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }

    @Test
    void createWithSameHubsReturns400WithFieldError() throws Exception {
        mvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originHub":"HUB-001","destinationHub":"HUB-001","serviceLevel":"STANDARD"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.errors[0].message").value("originHub and destinationHub must differ"));
    }
}
