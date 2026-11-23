package com.parcelplus.shipment.api;

import com.parcelplus.shipment.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ShipmentControllerIT {

    @Autowired
    MockMvc mvc;

    private static String body(String origin, String destination, String level) {
        return """
                {"originHub":%s,"destinationHub":%s,"serviceLevel":%s}"""
                .formatted(quote(origin), quote(destination), quote(level));
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

    @Test
    void createThenGetRoundTrip() throws Exception {
        MvcResult created = mvc.perform(post("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("HUB-001", "HUB-002", "EXPRESS")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.lastSequence").value(0))
                .andExpect(jsonPath("$.trackingCode").value(org.hamcrest.Matchers.matchesPattern("PP[0-9A-F]{10}")))
                .andReturn();

        String location = created.getResponse().getHeader("Location");
        assertThat(location).matches(".*/api/shipments/[0-9a-f-]{36}");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originHub").value("HUB-001"))
                .andExpect(jsonPath("$.destinationHub").value("HUB-002"))
                .andExpect(jsonPath("$.serviceLevel").value("EXPRESS"));
    }

    @Test
    void unknownIdReturns404() throws Exception {
        mvc.perform(get("/api/shipments/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsBadHubFormat() throws Exception {
        mvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content(body("PARIS", "HUB-002", "STANDARD")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingServiceLevel() throws Exception {
        mvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content(body("HUB-001", "HUB-002", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsSameOriginAndDestination() throws Exception {
        mvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content(body("HUB-001", "HUB-001", "STANDARD")))
                .andExpect(status().isBadRequest());
    }
}
