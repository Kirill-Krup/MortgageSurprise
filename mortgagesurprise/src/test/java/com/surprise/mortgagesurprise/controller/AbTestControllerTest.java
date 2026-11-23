package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.adapter.AbTestAdapter;
import com.surprise.mortgagesurprise.dto.abtest.AbTestAssignmentRequest;
import com.surprise.mortgagesurprise.dto.abtest.AbTestAssignmentResponse;
import com.surprise.mortgagesurprise.dto.abtest.AbTestStatsResponse;
import com.surprise.mortgagesurprise.dto.abtest.VariantStat;
import com.surprise.mortgagesurprise.model.entity.Role;
import com.surprise.mortgagesurprise.model.entity.User;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbTestControllerTest {

    @Mock
    private AbTestAdapter abTestAdapter;

    @InjectMocks
    private AbTestController abTestController;

    private CustomUserDetails userDetails;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        User user = new User("tester@bank.com", "pass123", "Tester", "User", Role.ROLE_USER);
        user.setId(testUserId);
        userDetails = new CustomUserDetails(user);
    }

    @Test
    @DisplayName("getVariant should cleanly delegate to service with userId when authenticated")
    void shouldGetVariantForAuthenticatedUser() {
        AbTestAssignmentResponse expected = new AbTestAssignmentResponse(testUserId, "mortgage_calculate_button", "emerald");
        when(abTestAdapter.getOrCreateVariant(userDetails, "mortgage_calculate_button")).thenReturn(expected);

        ResponseEntity<AbTestAssignmentResponse> response = abTestController.getVariant("mortgage_calculate_button", userDetails);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(expected);
        verify(abTestAdapter).getOrCreateVariant(userDetails, "mortgage_calculate_button");
    }

    @Test
    @DisplayName("getVariant should pass null userId to service when unauthenticated")
    void shouldGetVariantForGuestUser() {
        AbTestAssignmentResponse expected = new AbTestAssignmentResponse(null, "mortgage_calculate_button", "blue");
        when(abTestAdapter.getOrCreateVariant(null, "mortgage_calculate_button")).thenReturn(expected);

        ResponseEntity<AbTestAssignmentResponse> response = abTestController.getVariant("mortgage_calculate_button", null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(expected);
        verify(abTestAdapter).getOrCreateVariant(null, "mortgage_calculate_button");
    }

    @Test
    @DisplayName("assignVariant should cleanly delegate to service")
    void shouldAssignVariant() {
        AbTestAssignmentRequest request = new AbTestAssignmentRequest("mortgage_calculate_button", "emerald");
        AbTestAssignmentResponse expected = new AbTestAssignmentResponse(testUserId, "mortgage_calculate_button", "emerald");

        when(abTestAdapter.getOrCreateVariant(userDetails, "mortgage_calculate_button")).thenReturn(expected);

        ResponseEntity<AbTestAssignmentResponse> response = abTestController.assignVariant(request, userDetails);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(expected);
        verify(abTestAdapter).getOrCreateVariant(userDetails, "mortgage_calculate_button");
    }

    @Test
    @DisplayName("getStats should return aggregated statistics from service")
    void shouldReturnStats() {
        AbTestStatsResponse mockStats = new AbTestStatsResponse(
                "mortgage_calculate_button",
                2,
                5,
                List.of(new VariantStat("blue", 1, 3, 3.0), new VariantStat("emerald", 1, 2, 2.0)),
                List.of()
        );

        when(abTestAdapter.getStats("mortgage_calculate_button")).thenReturn(mockStats);

        ResponseEntity<AbTestStatsResponse> response = abTestController.getStats("mortgage_calculate_button");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(mockStats);
        verify(abTestAdapter).getStats("mortgage_calculate_button");
    }

    @Test
    @DisplayName("getAllExperiments should delegate to abTestAdapter")
    void shouldGetAllExperiments() {
        var exp = new com.surprise.mortgagesurprise.dto.abtest.ExperimentConfigDTO(
                "mortgage_calculate_button", "Name", "Desc", List.of(), "ACTIVE", false, null, null
        );
        when(abTestAdapter.getAllExperiments()).thenReturn(List.of(exp));

        var response = abTestController.getAllExperiments();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsExactly(exp);
        verify(abTestAdapter).getAllExperiments();
    }

    @Test
    @DisplayName("saveExperiment should delegate to abTestAdapter")
    void shouldSaveExperiment() {
        var exp = new com.surprise.mortgagesurprise.dto.abtest.ExperimentConfigDTO(
                "mortgage_calculate_button", "Name", "Desc", List.of(), "ACTIVE", false, null, null
        );
        when(abTestAdapter.saveExperiment(exp)).thenReturn(exp);

        var response = abTestController.saveExperiment(exp);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(exp);
        verify(abTestAdapter).saveExperiment(exp);
    }
}
