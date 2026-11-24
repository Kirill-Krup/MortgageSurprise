package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.admin.AdminLoanApplicationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdminLoanApplicationControllerTest {

    private AdminLoanApplicationController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminLoanApplicationController();
    }

    @Test
    @DisplayName("getLoanApplications should return empty list stub")
    void shouldReturnEmptyListStub() {
        ResponseEntity<List<AdminLoanApplicationDto>> response = controller.getLoanApplications();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("approveApplication should return approved status")
    void shouldApproveApplication() {
        ResponseEntity<Map<String, Object>> response = controller.approveApplication("MS-12345");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("success", true);
        assertThat(response.getBody()).containsEntry("status", "APPROVED");
        assertThat(response.getBody()).containsEntry("applicationId", "MS-12345");
    }

    @Test
    @DisplayName("rejectApplication should return rejected status")
    void shouldRejectApplication() {
        ResponseEntity<Map<String, Object>> response = controller.rejectApplication("MS-12345", Map.of("reason", "Low credit score"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("success", true);
        assertThat(response.getBody()).containsEntry("status", "REJECTED");
        assertThat(response.getBody()).containsEntry("reason", "Low credit score");
    }
}
