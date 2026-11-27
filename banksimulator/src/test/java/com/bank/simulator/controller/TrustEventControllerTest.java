package com.bank.simulator.controller;

import com.bank.simulator.config.SecurityConfig;
import com.surprise.common.dto.simulator.TrustEventResponse;
import com.surprise.common.dto.simulator.TrustEventType;
import com.bank.simulator.service.TrustEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrustEventController.class)
@Import(SecurityConfig.class)
@DisplayName("TrustEventController WebMvc Tests")
class TrustEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrustEventService trustEventService;

    @Test
    @DisplayName("POST /api/v1/events returns 201 Created for valid event")
    void registerEvent_ValidRequest_Returns201() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        TrustEventResponse response = TrustEventResponse.builder()
                .id(eventId)
                .subjectId(subjectId)
                .eventKey("evt-100")
                .eventType(TrustEventType.PAYMENT_ON_TIME)
                .delta(15)
                .scoreAfter(715)
                .source("mortgagesurprise")
                .occurredAt(Instant.parse("2026-03-01T10:00:00Z"))
                .build();

        when(trustEventService.registerEvent(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventKey": "evt-100",
                                  "documentHash": "hash-100",
                                  "eventType": "PAYMENT_ON_TIME",
                                  "source": "mortgagesurprise"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.eventKey").value("evt-100"))
                .andExpect(jsonPath("$.scoreAfter").value(715));
    }

    @Test
    @DisplayName("POST /api/v1/events returns 400 Bad Request when required fields are missing")
    void registerEvent_MissingRequiredFields_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventKey": "",
                                  "documentHash": "",
                                  "source": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isArray());
    }
}
