package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.audit.BankOfferSelectionRequest;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.enums.*;
import com.surprise.mortgagesurprise.repository.mongo.UserActionAuditMongoRepository;
import com.surprise.mortgagesurprise.testutil.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAuditServiceImpl Tests")
class UserAuditServiceImplTest {

    @Mock
    private UserActionAuditMongoRepository auditRepository;

    private UserAuditServiceImpl auditService;

    @BeforeEach
    void setUp() {
        auditService = new UserAuditServiceImpl(auditRepository);
    }

    @Nested
    @DisplayName("Enum Serialization Tests")
    class EnumSerializationTests {

        @ParameterizedTest(name = "Input \"{0}\" should map to {1}")
        @CsvSource({
                "MONTHLY, RECURRING",
                "monthly, RECURRING",
                "RECURRING, RECURRING",
                "ONE_TIME, ONE_TIME"
        })
        @DisplayName("Should correctly deserialize case-insensitive string to EarlyRepaymentType")
        void fromString_shouldMapToEarlyRepaymentType_whenStringValueProvided(String input, EarlyRepaymentType expected) {
            // Arrange & Act
            EarlyRepaymentType actual = EarlyRepaymentType.fromString(input);

            // Assert
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Mortgage Calculation Audit")
    class MortgageAuditTests {

        @Test
        @DisplayName("Should audit mortgage calculation for authorized non-admin user")
        void auditMortgageCalculation_shouldPersistAuditDocument_whenAuthorizedNonAdminUser() {
            // Arrange
            UUID userId = TestConstants.USER_ID;
            String userEmail = TestConstants.USER_EMAIL;

            CalculationRequest req = new CalculationRequest(
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.DOWN_PAYMENT,
                    TestConstants.TERM_MONTHS,
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
                    TestConstants.SCHEDULE_ID,
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.DOWN_PAYMENT,
                    TestConstants.LOAN_AMOUNT,
                    TestConstants.TERM_MONTHS,
                    TestConstants.TERM_MONTHS,
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

            // Act
            auditService.auditMortgageCalculation(userId, userEmail, false, req, res);

            // Assert
            ArgumentCaptor<UserActionAuditDocument> captor = ArgumentCaptor.forClass(UserActionAuditDocument.class);
            verify(auditRepository, times(1)).save(captor.capture());

            UserActionAuditDocument doc = captor.getValue();
            assertAll("Mortgage audit document verification",
                    () -> assertThat(doc.getUserId()).isEqualTo(userId),
                    () -> assertThat(doc.getUserEmail()).isEqualTo(userEmail),
                    () -> assertThat(doc.getActionType()).isEqualTo(UserAuditActionType.MORTGAGE_CALCULATION),
                    () -> assertThat(doc.getBankOfferId()).isEqualTo(101L),
                    () -> assertThat(doc.getBankName()).isEqualTo("СберБанк"),
                    () -> assertThat(doc.getLoanAmount()).isEqualByComparingTo(TestConstants.LOAN_AMOUNT)
            );
        }

        @Test
        @DisplayName("Should NOT audit actions when user is guest (null userId) or admin")
        void auditMortgageCalculation_shouldNotAudit_whenUserIsGuestOrAdmin() {
            // Arrange
            CalculationRequest req = new CalculationRequest(
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.DOWN_PAYMENT,
                    TestConstants.TERM_MONTHS,
                    new BigDecimal("8.50"),
                    PaymentType.ANNUITY,
                    Currency.RUB,
                    LocalDate.now(),
                    Collections.emptyList()
            );

            CalculationResponse res = new CalculationResponse(
                    TestConstants.SCHEDULE_ID,
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.DOWN_PAYMENT,
                    TestConstants.LOAN_AMOUNT,
                    TestConstants.TERM_MONTHS,
                    TestConstants.TERM_MONTHS,
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

            // Act - Guest (null userId)
            auditService.auditMortgageCalculation(null, null, false, req, res);

            // Act - Admin
            auditService.auditMortgageCalculation(UUID.randomUUID(), "admin@bank.com", true, req, res);

            // Assert
            verify(auditRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Bank Offer and Microloan Audit")
    class OtherAuditActions {

        @Test
        @DisplayName("Should audit bank offer selection for authenticated user")
        void auditBankOfferSelected_shouldPersistAuditDocument_whenAuthorizedUser() {
            // Arrange
            UUID userId = TestConstants.USER_ID;
            String userEmail = TestConstants.USER_EMAIL;

            BankOfferSelectionRequest request = new BankOfferSelectionRequest(
                    42L,
                    "ВТБ",
                    "Ипотека для IT",
                    new BigDecimal("6.00"),
                    new BigDecimal("20.00"),
                    360,
                    Currency.RUB
            );

            // Act
            auditService.auditBankOfferSelected(userId, userEmail, false, request);

            // Assert
            ArgumentCaptor<UserActionAuditDocument> captor = ArgumentCaptor.forClass(UserActionAuditDocument.class);
            verify(auditRepository, times(1)).save(captor.capture());

            UserActionAuditDocument doc = captor.getValue();
            assertAll("Bank offer selection audit document",
                    () -> assertThat(doc.getUserId()).isEqualTo(userId),
                    () -> assertThat(doc.getUserEmail()).isEqualTo(userEmail),
                    () -> assertThat(doc.getActionType()).isEqualTo(UserAuditActionType.BANK_OFFER_SELECTED),
                    () -> assertThat(doc.getBankOfferId()).isEqualTo(42L),
                    () -> assertThat(doc.getBankName()).isEqualTo("ВТБ"),
                    () -> assertThat(doc.getOfferName()).isEqualTo("Ипотека для IT"),
                    () -> assertThat(doc.getInterestRate()).isEqualByComparingTo("6.00")
            );
        }

        @Test
        @DisplayName("Should audit microloan application for authenticated user")
        void auditMicroloanApplication_shouldPersistAuditDocument_whenAuthorizedUser() {
            // Arrange
            UUID userId = TestConstants.USER_ID;
            String userEmail = TestConstants.USER_EMAIL;

            MicroloanApplicationRequest request = new MicroloanApplicationRequest(
                    new BigDecimal("20000.00"),
                    14,
                    "Покупка продуктов питания",
                    "BASIC_NEEDS",
                    new BigDecimal("22240.00"),
                    TestConstants.SCHEDULE_ID,
                    "MS-77123",
                    "ИВАНОВ ИВАН ИВАНОВИЧ",
                    Currency.RUB
            );

            // Act
            auditService.auditMicroloanApplication(userId, userEmail, false, request);

            // Assert
            ArgumentCaptor<UserActionAuditDocument> captor = ArgumentCaptor.forClass(UserActionAuditDocument.class);
            verify(auditRepository, times(1)).save(captor.capture());

            UserActionAuditDocument doc = captor.getValue();
            assertAll("Microloan application audit document",
                    () -> assertThat(doc.getUserId()).isEqualTo(userId),
                    () -> assertThat(doc.getUserEmail()).isEqualTo(userEmail),
                    () -> assertThat(doc.getActionType()).isEqualTo(UserAuditActionType.MICROLOAN_APPLICATION),
                    () -> assertThat(doc.getLoanAmount()).isEqualByComparingTo("20000.00"),
                    () -> assertThat(doc.getTermDays()).isEqualTo(14),
                    () -> assertThat(doc.getApplicationId()).isEqualTo("MS-77123"),
                    () -> assertThat(doc.getMicroloanType()).isEqualTo("BASIC_NEEDS")
            );
        }
    }
}
