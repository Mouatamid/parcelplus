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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Status-change endpoint against real Postgres. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ShipmentStatusChangeIT {

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
}
