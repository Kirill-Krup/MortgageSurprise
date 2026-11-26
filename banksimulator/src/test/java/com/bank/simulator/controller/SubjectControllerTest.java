package com.bank.simulator.controller;

import com.bank.simulator.config.SecurityConfig;
import com.surprise.common.dto.simulator.SubjectResponse;
import com.bank.simulator.service.SubjectService;
import com.surprise.common.dto.simulator.RiskLevel;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubjectController.class)
@Import(SecurityConfig.class)
@DisplayName("SubjectController WebMvc Tests")
class SubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubjectService subjectService;

    @Test
    @DisplayName("GET /api/v1/subjects returns 200 OK with paginated subjects")
    void getAllSubjects_ReturnsList() throws Exception {
        UUID id = UUID.randomUUID();
        SubjectResponse subject = SubjectResponse.builder()
                .id(id)
                .trustScore(700)
                .hardBlock(false)
                .riskLevel(RiskLevel.LOW)
                .blacklisted(false)
                .documents(Collections.emptyList())
                .build();

        when(subjectService.getAll(0, 20)).thenReturn(List.of(subject));

        mockMvc.perform(get("/api/v1/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].trustScore").value(700));
    }

    @Test
    @DisplayName("GET /api/v1/subjects/{id} returns 404 Not Found when subject does not exist")
    void getSubjectById_NotFound_Returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(subjectService.getById(id)).thenThrow(new EntityNotFoundException("Subject not found by id: " + id));

        mockMvc.perform(get("/api/v1/subjects/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/v1/subjects returns 201 Created for valid payload")
    void createSubject_ValidPayload_Returns201() throws Exception {
        UUID id = UUID.randomUUID();
        SubjectResponse created = SubjectResponse.builder()
                .id(id)
                .trustScore(700)
                .hardBlock(false)
                .riskLevel(RiskLevel.LOW)
                .blacklisted(false)
                .documents(Collections.emptyList())
                .build();

        when(subjectService.createSubject(any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "PASSPORT",
                                  "documentHash": "hash-123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/subjects returns 400 Bad Request when validation fails")
    void createSubject_InvalidPayload_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": null,
                                  "documentHash": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/subjects/{id}/hard-block returns 200 OK when reason is valid")
    void hardBlock_ValidReason_Returns200() throws Exception {
        UUID id = UUID.randomUUID();
        SubjectResponse blocked = SubjectResponse.builder()
                .id(id)
                .trustScore(700)
                .hardBlock(true)
                .blockReason("Suspected fraud")
                .riskLevel(RiskLevel.HIGH)
                .blacklisted(true)
                .documents(Collections.emptyList())
                .build();

        when(subjectService.hardBlock(eq(id), any())).thenReturn(blocked);

        mockMvc.perform(post("/api/v1/subjects/{id}/hard-block", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Suspected fraud"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hardBlock").value(true))
                .andExpect(jsonPath("$.blockReason").value("Suspected fraud"));
    }

    @Test
    @DisplayName("DELETE /api/v1/subjects/{id} returns 204 No Content on success and 404 when missing")
    void deleteSubject_SuccessAndNotFound() throws Exception {
        UUID existingId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();

        doNothing().when(subjectService).delete(existingId);
        doThrow(new EntityNotFoundException("Subject not found")).when(subjectService).delete(missingId);

        mockMvc.perform(delete("/api/v1/subjects/{id}", existingId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/subjects/{id}", missingId))
                .andExpect(status().isNotFound());
    }
}
