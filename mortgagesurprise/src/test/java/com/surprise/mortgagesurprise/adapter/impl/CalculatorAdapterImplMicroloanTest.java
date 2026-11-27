package com.surprise.mortgagesurprise.adapter.impl;

import com.surprise.common.dto.scoring.CreditDecisionResponse;
import com.surprise.common.dto.simulator.BlacklistCheckResponse;
import com.surprise.common.dto.simulator.RiskLevel;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationRequest;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationResponse;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.entity.Role;
import com.surprise.mortgagesurprise.model.entity.User;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.BankSimulatorIntegrationService;
import com.surprise.mortgagesurprise.service.CalculatorService;
import com.surprise.mortgagesurprise.service.CreditScoringIntegrationService;
import com.surprise.mortgagesurprise.service.MicroloanService;
import com.surprise.mortgagesurprise.service.UserAuditService;
import com.surprise.mortgagesurprise.service.impl.MicroloanApplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalculatorAdapterImplMicroloanTest {

    @Mock
    private CalculatorService calculatorService;

    @Mock
    private MicroloanService microloanService;

    @Mock
    private UserAuditService userAuditService;

    @Mock
    private BankSimulatorIntegrationService bankSimulatorIntegrationService;

    @Mock
    private CreditScoringIntegrationService creditScoringIntegrationService;

    private CalculatorAdapterImpl calculatorAdapter;

    @BeforeEach
    void setUp() {
        var microloanApplicationService = new MicroloanApplicationServiceImpl(
                userAuditService,
                bankSimulatorIntegrationService,
                creditScoringIntegrationService
        );
        calculatorAdapter = new CalculatorAdapterImpl(
                calculatorService,
                microloanService,
                microloanApplicationService,
                userAuditService
        );
    }

    private CustomUserDetails user(UUID id) {
        User u = new User("user@example.com", "pass", "Ivan", "Ivanov", Role.ROLE_USER);
        u.setId(id);
        return new CustomUserDetails(u);
    }

    @Test
    @DisplayName("First microloan triggers subject creation in banksimulator")
    void applyMicroloan_firstLoan_createsSubject() {
        UUID userId = UUID.randomUUID();
        when(bankSimulatorIntegrationService.checkBlacklist(userId)).thenReturn(Optional.empty());
        when(creditScoringIntegrationService.evaluateScoring(userId, "user@example.com", "ROLE_USER"))
                .thenReturn(new CreditDecisionResponse(true, null, 0.8f));
        when(userAuditService.hasMicroloanApplications(userId)).thenReturn(false);

        MicroloanApplicationRequest req = new MicroloanApplicationRequest(
                BigDecimal.valueOf(10000), 14, "Groceries", "BASIC",
                BigDecimal.valueOf(11000), "sched-1", "MS-11111", "Ivan Ivanov", Currency.RUB
        );

        MicroloanApplicationResponse resp = calculatorAdapter.applyMicroloan(req, user(userId));

        assertThat(resp.success()).isTrue();
        verify(bankSimulatorIntegrationService).checkBlacklist(userId);
        verify(creditScoringIntegrationService).evaluateScoring(userId, "user@example.com", "ROLE_USER");
        verify(userAuditService).auditMicroloanApplication(eq(userId), eq("user@example.com"), eq(false), any());
        verify(bankSimulatorIntegrationService).ensureSubjectCreatedForFirstMicroloan(userId);
    }

    @Test
    @DisplayName("Subsequent microloan does NOT trigger subject creation")
    void applyMicroloan_subsequentLoan_doesNotCreateSubject() {
        UUID userId = UUID.randomUUID();
        when(bankSimulatorIntegrationService.checkBlacklist(userId)).thenReturn(Optional.empty());
        when(creditScoringIntegrationService.evaluateScoring(userId, "user@example.com", "ROLE_USER"))
                .thenReturn(new CreditDecisionResponse(true, null, 0.7f));
        when(userAuditService.hasMicroloanApplications(userId)).thenReturn(true);

        MicroloanApplicationRequest req = new MicroloanApplicationRequest(
                BigDecimal.valueOf(10000), 14, "Groceries", "BASIC",
                BigDecimal.valueOf(11000), "sched-1", "MS-22222", "Ivan Ivanov", Currency.RUB
        );

        calculatorAdapter.applyMicroloan(req, user(userId));

        verify(bankSimulatorIntegrationService, never()).ensureSubjectCreatedForFirstMicroloan(any());
    }

    @Test
    @DisplayName("Blacklisted user is rejected before credit scoring")
    void applyMicroloan_blacklisted_rejectedWithoutScoring() {
        UUID userId = UUID.randomUUID();
        when(bankSimulatorIntegrationService.checkBlacklist(userId))
                .thenReturn(Optional.of(new BlacklistCheckResponse(
                        true, 10, RiskLevel.HIGH, true, List.of("Fraud history"))));

        MicroloanApplicationRequest req = new MicroloanApplicationRequest(
                BigDecimal.valueOf(10000), 14, "Groceries", "BASIC",
                BigDecimal.valueOf(11000), "sched-1", "MS-33333", "Ivan Ivanov", Currency.RUB
        );

        MicroloanApplicationResponse resp = calculatorAdapter.applyMicroloan(req, user(userId));

        assertThat(resp.success()).isFalse();
        assertThat(resp.status()).isEqualTo("REJECTED");
        assertThat(resp.message()).contains("Fraud history");
        verify(creditScoringIntegrationService, never()).evaluateScoring(any(), any(), any());
        verify(userAuditService, never()).auditMicroloanApplication(any(), any(), anyBoolean(), any());
    }

    @Test
    @DisplayName("Non-blacklisted user rejected by credit scoring does not get loan")
    void applyMicroloan_scoringRejected_rejectedWithoutAudit() {
        UUID userId = UUID.randomUUID();
        when(bankSimulatorIntegrationService.checkBlacklist(userId))
                .thenReturn(Optional.of(new BlacklistCheckResponse(
                        false, 80, RiskLevel.LOW, false, List.of())));
        when(creditScoringIntegrationService.evaluateScoring(userId, "user@example.com", "ROLE_USER"))
                .thenReturn(new CreditDecisionResponse(false, "Your credit score is too low", 0f));

        MicroloanApplicationRequest req = new MicroloanApplicationRequest(
                BigDecimal.valueOf(10000), 14, "Groceries", "BASIC",
                BigDecimal.valueOf(11000), "sched-1", "MS-44444", "Ivan Ivanov", Currency.RUB
        );

        MicroloanApplicationResponse resp = calculatorAdapter.applyMicroloan(req, user(userId));

        assertThat(resp.success()).isFalse();
        assertThat(resp.status()).isEqualTo("REJECTED");
        assertThat(resp.message()).isEqualTo("Your credit score is too low");
        verify(userAuditService, never()).auditMicroloanApplication(any(), any(), anyBoolean(), any());
        verify(bankSimulatorIntegrationService, never()).ensureSubjectCreatedForFirstMicroloan(any());
    }

    @Test
    @DisplayName("Repaying microloan on time sends positive trust event")
    void repayMicroloan_onTime_recordsOnTimeRepayment() {
        UUID userId = UUID.randomUUID();
        UserActionAuditDocument doc = new UserActionAuditDocument();
        doc.setApplicationId("MS-11111");
        doc.setUserId(userId);
        doc.setActionType(UserAuditActionType.MICROLOAN_APPLICATION);
        doc.setTermDays(14);
        doc.setTimestamp(Instant.now().minus(5, ChronoUnit.DAYS));
        doc.setMetadata(new HashMap<>());

        when(userAuditService.findMicroloanApplication(userId, "MS-11111")).thenReturn(Optional.of(doc));

        MicroloanApplicationResponse resp = calculatorAdapter.repayMicroloan("MS-11111", user(userId));

        assertThat(resp.status()).isEqualTo("PAID_OFF");
        assertThat(doc.getMetadata().get("status")).isEqualTo("PAID_OFF");
        verify(userAuditService).saveAudit(doc);
        verify(bankSimulatorIntegrationService).recordMicroloanRepayment(userId, "MS-11111", false, 0);
    }

    @Test
    @DisplayName("Repaying overdue microloan sends negative trust event with overdue days")
    void repayMicroloan_overdue_recordsOverdueRepayment() {
        UUID userId = UUID.randomUUID();
        UserActionAuditDocument doc = new UserActionAuditDocument();
        doc.setApplicationId("MS-33333");
        doc.setUserId(userId);
        doc.setActionType(UserAuditActionType.MICROLOAN_APPLICATION);
        doc.setTermDays(14);
        doc.setTimestamp(Instant.now().minus(20, ChronoUnit.DAYS));
        doc.setMetadata(new HashMap<>());

        when(userAuditService.findMicroloanApplication(userId, "MS-33333")).thenReturn(Optional.of(doc));

        MicroloanApplicationResponse resp = calculatorAdapter.repayMicroloan("MS-33333", user(userId));

        assertThat(resp.status()).isEqualTo("PAID_OFF_OVERDUE");
        verify(bankSimulatorIntegrationService).recordMicroloanRepayment(eq(userId), eq("MS-33333"), eq(true), longThat(d -> d >= 5));
    }
}
