package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.entity.Role;
import com.surprise.mortgagesurprise.model.entity.User;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.UserAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuditControllerTest {

    @Mock
    private UserAuditService userAuditService;

    @InjectMocks
    private UserAuditController userAuditController;

    private CustomUserDetails userDetails;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        User user = new User("test@example.com", "secret123", "Test", "User", Role.ROLE_USER);
        user.setId(testUserId);
        userDetails = new CustomUserDetails(user);
    }

    @Test
    @DisplayName("GET /api/v1/profile/activity returns 200 OK with empty list when userDetails is null")
    void getMyActivity_WhenUserDetailsNull_ReturnsEmptyList() {
        ResponseEntity<List<UserActionAuditDocument>> response = userAuditController.getMyActivity(null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/profile/activity returns 200 OK with empty list when user has no activity records")
    void getMyActivity_WhenNoAudits_ReturnsEmptyList() {
        when(userAuditService.getAuditsForUser(testUserId)).thenReturn(Collections.emptyList());

        ResponseEntity<List<UserActionAuditDocument>> response = userAuditController.getMyActivity(userDetails, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/profile/activity returns records and filters by action including aliases")
    void getMyActivity_FiltersByActionAndAlias() {
        UserActionAuditDocument doc1 = new UserActionAuditDocument(
                "doc-1", testUserId, "test@example.com", UserAuditActionType.MICROLOAN_APPLICATION,
                "Applied for microloan", null, null, null, new BigDecimal("10000"), null, null, null,
                14, null, null, new BigDecimal("11000"), null, "BASIC_NEEDS", "Groceries", "MS-12345",
                Currency.RUB, Map.of(), Instant.now()
        );
        UserActionAuditDocument doc2 = new UserActionAuditDocument(
                "doc-2", testUserId, "test@example.com", UserAuditActionType.MORTGAGE_CALCULATION,
                "Calculated mortgage", null, null, null, new BigDecimal("5000000"), null, null, 240,
                null, new BigDecimal("10.5"), null, null, null, null, null, null,
                Currency.RUB, Map.of(), Instant.now()
        );

        when(userAuditService.getAuditsForUser(testUserId)).thenReturn(List.of(doc1, doc2));

        // No action filter -> all 2 records returned
        ResponseEntity<List<UserActionAuditDocument>> allRes = userAuditController.getMyActivity(userDetails, null);
        assertThat(allRes.getBody()).hasSize(2);

        // Filter by MICROLOAN_APPLIED (alias for MICROLOAN_APPLICATION) -> returns doc1
        ResponseEntity<List<UserActionAuditDocument>> filterRes = userAuditController.getMyActivity(userDetails, UserAuditActionType.MICROLOAN_APPLIED);
        assertThat(filterRes.getBody()).hasSize(1);
        assertThat(filterRes.getBody().get(0).getId()).isEqualTo("doc-1");

        // Filter by SCENARIO_CALCULATED (alias for MORTGAGE_CALCULATION) -> returns doc2
        ResponseEntity<List<UserActionAuditDocument>> filterRes2 = userAuditController.getMyActivity(userDetails, UserAuditActionType.SCENARIO_CALCULATED);
        assertThat(filterRes2.getBody()).hasSize(1);
        assertThat(filterRes2.getBody().get(0).getId()).isEqualTo("doc-2");
    }
}
