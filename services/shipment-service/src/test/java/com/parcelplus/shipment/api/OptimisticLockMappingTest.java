package com.parcelplus.shipment.api;

import com.parcelplus.shipment.application.ShipmentApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A real optimistic-lock clash needs two concurrent transactions, which makes a flaky test.
 * Instead the service is mocked to throw the exception Spring raises, and we test the mapping.
 * No database, so this is a *Test and runs with "mvn test".
 */
@WebMvcTest(ShipmentController.class)
class OptimisticLockMappingTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ShipmentApplicationService service;

    @Test
    void optimisticLockFailureReturns409Problem() throws Exception {
        when(service.changeStatus(any(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Object.class, "id"));

        mvc.perform(post("/api/shipments/" + UUID.randomUUID() + "/status-changes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PICKED_UP","hubCode":"HUB-001"}"""))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Concurrent modification"));
    }
}
