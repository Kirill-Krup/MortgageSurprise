package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.audit.BankOfferSelectionRequest;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.enums.*;
import com.surprise.mortgagesurprise.repository.mongo.UserActionAuditMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuditServiceImplTest {

    @Mock
    private UserActionAuditMongoRepository auditRepository;

    private UserAuditServiceImpl auditService;

    @BeforeEach
    void setUp() {
        auditService = new UserAuditServiceImpl(auditRepository);
    }

    @Test
    @DisplayName("Should deserialize MONTHLY string to EarlyRepaymentType.RECURRING without error")
    void shouldDeserializeMonthlyToRecurring() {
        assertThat(EarlyRepaymentType.fromString("MONTHLY")).isEqualTo(EarlyRepaymentType.RECURRING);
        assertThat(EarlyRepaymentType.fromString("monthly")).isEqualTo(EarlyRepaymentType.RECURRING);
        assertThat(EarlyRepaymentType.fromString("RECURRING")).isEqualTo(EarlyRepaymentType.RECURRING);
        assertThat(EarlyRepaymentType.fromString("ONE_TIME")).isEqualTo(EarlyRepaymentType.ONE_TIME);
    }

    @Test
    @DisplayName("Should audit mortgage calculation for authorized non-admin user")
    void shouldAuditMortgageCalculation() {
        UUID userId = UUID.randomUUID();
        String userEmail = "client@example.com";

        CalculationRequest req = new CalculationRequest(
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                240,
                new BigDecimal("8.50"),
                PaymentType.ANNUITY,
                Currency.RUB,
                LocalDate.now(),
                Collections.emptyList(),
                101L,
                "СберБанк",
                "Семейная ипотека"
        );

        CalculationResponse res = new CalculationResponse(
                "sched-1",
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                new BigDecimal("4000000.00"),
                240,
                240,
                new BigDecimal("8.50"),
                "ANNUITY",
                Currency.RUB,
                new BigDecimal("34712.98"),
                new BigDecimal("34712.98"),
                new BigDecimal("34712.98"),
                new BigDecimal("4331115.20"),
                new BigDecimal("8331115.20"),
                Collections.emptyList()
        );

        auditService.auditMortgageCalculation(userId, userEmail, false, req, res);

        ArgumentCaptor<UserActionAuditDocument> captor = ArgumentCaptor.forClass(UserActionAuditDocument.class);
        verify(auditRepository, times(1)).save(captor.capture());

        UserActionAuditDocument doc = captor.getValue();
        assertThat(doc.getUserId()).isEqualTo(userId);
        assertThat(doc.getUserEmail()).isEqualTo(userEmail);
        assertThat(doc.getActionType()).isEqualTo(UserAuditActionType.MORTGAGE_CALCULATION);
        assertThat(doc.getBankOfferId()).isEqualTo(101L);
        assertThat(doc.getBankName()).isEqualTo("СберБанк");
        assertThat(doc.getLoanAmount()).isEqualByComparingTo("4000000.00");
    }

    @Test
    @DisplayName("Should NOT audit actions when user is guest (null userId) or admin")
    void shouldNotAuditGuestOrAdmin() {
        CalculationRequest req = new CalculationRequest(
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                240,
                new BigDecimal("8.50"),
                PaymentType.ANNUITY,
                Currency.RUB,
                LocalDate.now(),
                Collections.emptyList()
        );

        CalculationResponse res = new CalculationResponse(
                "sched-1",
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                new BigDecimal("4000000.00"),
                240,
                240,
                new BigDecimal("8.50"),
                "ANNUITY",
                Currency.RUB,
                new BigDecimal("34712.98"),
                new BigDecimal("34712.98"),
                new BigDecimal("34712.98"),
                new BigDecimal("4331115.20"),
                new BigDecimal("8331115.20"),
                Collections.emptyList()
        );

        // Guest (null userId)
        auditService.auditMortgageCalculation(null, null, false, req, res);
        verify(auditRepository, never()).save(any());

        // Admin
        auditService.auditMortgageCalculation(UUID.randomUUID(), "admin@bank.com", true, req, res);
        verify(auditRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should audit bank offer selection")
    void shouldAuditBankOfferSelection() {
        UUID userId = UUID.randomUUID();
        String userEmail = "client@example.com";

        BankOfferSelectionRequest request = new BankOfferSelectionRequest(
                42L,
                "ВТБ",
                "Ипотека для IT",
                new BigDecimal("6.00"),
                new BigDecimal("20.00"),
                360,
                Currency.RUB
        );

        auditService.auditBankOfferSelected(userId, userEmail, false, request);

        ArgumentCaptor<UserActionAuditDocument> captor = ArgumentCaptor.forClass(UserActionAuditDocument.class);
        verify(auditRepository, times(1)).save(captor.capture());

        UserActionAuditDocument doc = captor.getValue();
        assertThat(doc.getUserId()).isEqualTo(userId);
        assertThat(doc.getUserEmail()).isEqualTo(userEmail);
        assertThat(doc.getActionType()).isEqualTo(UserAuditActionType.BANK_OFFER_SELECTED);
        assertThat(doc.getBankOfferId()).isEqualTo(42L);
        assertThat(doc.getBankName()).isEqualTo("ВТБ");
        assertThat(doc.getOfferName()).isEqualTo("Ипотека для IT");
        assertThat(doc.getInterestRate()).isEqualByComparingTo("6.00");
    }

    @Test
    @DisplayName("Should audit microloan application")
    void shouldAuditMicroloanApplication() {
        UUID userId = UUID.randomUUID();
        String userEmail = "client@example.com";

        MicroloanApplicationRequest request = new MicroloanApplicationRequest(
                new BigDecimal("20000.00"),
                14,
                "Покупка продуктов питания",
                "BASIC_NEEDS",
                new BigDecimal("22240.00"),
                "sched-micro-1",
                "MS-77123",
                "ИВАНОВ ИВАН ИВАНОВИЧ",
                Currency.RUB
        );

        auditService.auditMicroloanApplication(userId, userEmail, false, request);

        ArgumentCaptor<UserActionAuditDocument> captor = ArgumentCaptor.forClass(UserActionAuditDocument.class);
        verify(auditRepository, times(1)).save(captor.capture());

        UserActionAuditDocument doc = captor.getValue();
        assertThat(doc.getUserId()).isEqualTo(userId);
        assertThat(doc.getUserEmail()).isEqualTo(userEmail);
        assertThat(doc.getActionType()).isEqualTo(UserAuditActionType.MICROLOAN_APPLICATION);
        assertThat(doc.getLoanAmount()).isEqualByComparingTo("20000.00");
        assertThat(doc.getTermDays()).isEqualTo(14);
        assertThat(doc.getApplicationId()).isEqualTo("MS-77123");
        assertThat(doc.getMicroloanType()).isEqualTo("BASIC_NEEDS");
    }

    @Test
    @DisplayName("Should map and resolve audit action types and aliases properly")
    void shouldMapAndResolveAuditActionTypes() {
        assertThat(UserAuditActionType.fromString("MICROLOAN_APPLIED")).isEqualTo(UserAuditActionType.MICROLOAN_APPLICATION);
        assertThat(UserAuditActionType.fromString("microloan_applied")).isEqualTo(UserAuditActionType.MICROLOAN_APPLICATION);
        assertThat(UserAuditActionType.fromString("SCENARIO_CALCULATED")).isEqualTo(UserAuditActionType.MORTGAGE_CALCULATION);
        assertThat(UserAuditActionType.fromString("SCENARIO_SAVED")).isEqualTo(UserAuditActionType.SCENARIO_SAVED);
        assertThat(UserAuditActionType.MICROLOAN_APPLIED.canonical()).isEqualTo(UserAuditActionType.MICROLOAN_APPLICATION);
        assertThat(UserAuditActionType.SCENARIO_CALCULATED.canonical()).isEqualTo(UserAuditActionType.MORTGAGE_CALCULATION);
        assertThat(UserAuditActionType.SCENARIO_SAVED.canonical()).isEqualTo(UserAuditActionType.SCENARIO_SAVED);
    }

    @Test
    @DisplayName("Should return empty list safely when repository throws exception or returns null")
    void shouldReturnEmptyListWhenRepositoryFailsOrEmpty() {
        UUID userId = UUID.randomUUID();
        when(auditRepository.findByUserIdOrderByTimestampDesc(userId)).thenThrow(new RuntimeException("Mongo connection timeout"));
        assertThat(auditService.getAuditsForUser(userId)).isEmpty();

        assertThat(auditService.getAuditsForUser(null)).isEmpty();

        when(auditRepository.findAllByOrderByTimestampDesc()).thenThrow(new RuntimeException("Mongo unavailable"));
        assertThat(auditService.getAllAudits()).isEmpty();

        when(auditRepository.findByActionTypeOrderByTimestampDesc(any())).thenThrow(new RuntimeException("Mongo query error"));
        assertThat(auditService.getAuditsByAction(UserAuditActionType.MICROLOAN_APPLICATION)).isEmpty();
    }
}
