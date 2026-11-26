package com.bank.simulator.controller;

import com.bank.simulator.config.SecurityConfig;
import com.bank.simulator.service.BlacklistCheckService;
import com.surprise.common.dto.simulator.BlacklistCheckRequest;
import com.surprise.common.dto.simulator.BlacklistCheckResponse;
import com.surprise.common.dto.simulator.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BlacklistCheckController.class)
@Import(SecurityConfig.class)
@DisplayName("BlacklistCheckController WebMvc Tests")
class BlacklistCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlacklistCheckService blacklistCheckService;

    @Test
    @DisplayName("POST /api/v1/blacklist/check returns 200 OK with check result without auth")
    void checkSubject_ValidRequest_ReturnsOk() throws Exception {
        BlacklistCheckResponse response = BlacklistCheckResponse.builder()
                .blacklisted(true)
                .trustScore(200)
                .riskLevel(RiskLevel.HIGH)
                .hardBlock(false)
                .reasons(List.of("SCORE_BELOW_THRESHOLD: 200 < 300"))
                .build();

        when(blacklistCheckService.check(any(BlacklistCheckRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/blacklist/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "PASSPORT",
                                  "documentHash": "hash-abc"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blacklisted").value(true))
                .andExpect(jsonPath("$.trustScore").value(200))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.reasons[0]").value("SCORE_BELOW_THRESHOLD: 200 < 300"));
    }

    @Test
    @DisplayName("POST /api/v1/blacklist/check returns 400 Bad Request when service throws IllegalArgumentException")
    void checkSubject_MissingIdentifier_ReturnsBadRequest() throws Exception {
        when(blacklistCheckService.check(any(BlacklistCheckRequest.class)))
                .thenThrow(new IllegalArgumentException("Either documentHash or documentNumber must be provided"));

        mockMvc.perform(post("/api/v1/blacklist/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "PASSPORT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Either documentHash or documentNumber must be provided"));
    }
}
