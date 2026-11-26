package com.bank.simulator.integration;

import com.bank.simulator.BaseIntegrationTest;
import com.bank.simulator.repository.SubjectDocumentRepository;
import com.bank.simulator.repository.SubjectRepository;
import com.bank.simulator.repository.TrustEventRepository;
import com.bank.simulator.service.DocumentHashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("BankSimulator End-to-End Integration Tests")
class BankSimulatorIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SubjectDocumentRepository documentRepository;

    @Autowired
    private TrustEventRepository trustEventRepository;

    @Autowired
    private DocumentHashService documentHashService;

    @BeforeEach
    void cleanDatabase() {
        trustEventRepository.deleteAll();
        documentRepository.deleteAll();
        subjectRepository.deleteAll();
    }

    @Test
    @DisplayName("Creates subject, checks blacklist by documentNumber, and links additional document")
    void subjectLifecycleAndBlacklistCheck_ByDocumentNumber_WorksEndToEnd() throws Exception {
        String rawPassport = "4510 998877";
        String passportHash = documentHashService.hash(rawPassport);

        MvcResult createResult = mockMvc.perform(post("/api/v1/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "PASSPORT",
                                  "documentHash": "%s"
                                }
                                """.formatted(passportHash)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trustScore").value(700))
                .andExpect(jsonPath("$.blacklisted").value(false))
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andReturn();

        String body = createResult.getResponse().getContentAsString();
        String subjectId = com.jayway.jsonpath.JsonPath.read(body, "$.id");

        mockMvc.perform(post("/api/v1/blacklist/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "PASSPORT",
                                  "documentNumber": "4510-998877"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blacklisted").value(false))
                .andExpect(jsonPath("$.trustScore").value(700))
                .andExpect(jsonPath("$.riskLevel").value("LOW"));

        mockMvc.perform(post("/api/v1/subjects/{id}/documents", subjectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "PERSONAL_ID",
                                  "documentHash": "inn-hash-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents.length()").value(2));

        mockMvc.perform(get("/api/v1/subjects/by-document/{hash}", "inn-hash-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(subjectId));
    }

    @Test
    @DisplayName("Processes trust events idempotently and updates blacklist status when score drops below threshold")
    void trustEvents_IdempotencyAndScoreThreshold_PersistedInPostgres() throws Exception {
        String docHash = "lazy-subject-hash-1";

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventKey": "evt-overdue-1",
                                  "documentHash": "%s",
                                  "eventType": "OVERDUE_90_DAYS",
                                  "source": "core-bank"
                                }
                                """.formatted(docHash)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.delta").value(-200))
                .andExpect(jsonPath("$.scoreAfter").value(500));

        // Repeat with the same eventKey -> idempotent, score must stay 500
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventKey": "evt-overdue-1",
                                  "documentHash": "%s",
                                  "eventType": "OVERDUE_90_DAYS",
                                  "source": "core-bank"
                                }
                                """.formatted(docHash)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scoreAfter").value(500));

        // Second overdue drops score to 300, then another -200 drops to 100 (< 300 threshold)
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventKey": "evt-overdue-2",
                                  "documentHash": "%s",
                                  "eventType": "OVERDUE_90_DAYS",
                                  "delta": -250,
                                  "source": "core-bank"
                                }
                                """.formatted(docHash)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scoreAfter").value(250));

        mockMvc.perform(post("/api/v1/blacklist/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "PASSPORT",
                                  "documentHash": "%s"
                                }
                                """.formatted(docHash)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blacklisted").value(true))
                .andExpect(jsonPath("$.trustScore").value(250))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"));

        assertThat(trustEventRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Applies and removes hard block and deletes subject")
    void hardBlockUnblockAndDelete_WorksEndToEnd() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "PASSPORT",
                                  "documentHash": "block-test-hash"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String subjectId = com.jayway.jsonpath.JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/subjects/{id}/hard-block", subjectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Sanctions match"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hardBlock").value(true))
                .andExpect(jsonPath("$.blacklisted").value(true))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"));

        mockMvc.perform(post("/api/v1/subjects/{id}/unblock", subjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hardBlock").value(false))
                .andExpect(jsonPath("$.blacklisted").value(false))
                .andExpect(jsonPath("$.riskLevel").value("LOW"));

        mockMvc.perform(delete("/api/v1/subjects/{id}", subjectId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/subjects/{id}", subjectId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Returns 400 on duplicate document hash and 401 on protected admin endpoint without credentials")
    void errorPaths_DuplicateHashAndUnauthorizedAdmin_ReturnExpectedStatuses() throws Exception {
        mockMvc.perform(post("/api/v1/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "PASSPORT",
                                  "documentHash": "dup-hash-1"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "PASSPORT",
                                  "documentHash": "dup-hash-1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/v1/admin/anything"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/subjects/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
