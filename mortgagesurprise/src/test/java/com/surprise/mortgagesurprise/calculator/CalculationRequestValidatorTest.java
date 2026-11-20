package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.testutil.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("CalculationRequestValidator Tests")
class CalculationRequestValidatorTest {

    private CalculationRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CalculationRequestValidator();
    }

    @Nested
    @DisplayName("Valid Request Verification")
    class ValidRequests {

        @Test
        @DisplayName("Should pass validation without exceptions for valid calculation request")
        void validate_shouldPassValidation_whenRequestIsValid() {
            // Arrange
            CalculationRequest request = new CalculationRequest(
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.DOWN_PAYMENT,
                    TestConstants.TERM_MONTHS,
                    TestConstants.ANNUAL_RATE,
                    PaymentType.ANNUITY,
                    LocalDate.now(),
                    List.of()
            );

            // Act & Assert
            assertThatCode(() -> validator.validate(request))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Null and Boundary Validations")
    class BoundaryValidations {

        @Test
        @DisplayName("Should throw IllegalArgumentException when calculation request is null")
        void validate_shouldThrowIllegalArgumentException_whenRequestIsNull() {
            // Arrange
            CalculationRequest request = null;

            // Act & Assert
            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Calculation request cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when property price is null")
        void validate_shouldThrowIllegalArgumentException_whenPropertyPriceIsNull() {
            // Arrange
            CalculationRequest request = new CalculationRequest(
                    null,
                    TestConstants.DOWN_PAYMENT,
                    TestConstants.TERM_MONTHS,
                    TestConstants.ANNUAL_RATE,
                    PaymentType.ANNUITY,
                    LocalDate.now(),
                    List.of()
            );

            // Act & Assert
            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Property price must be greater than zero");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when property price is zero or negative")
        void validate_shouldThrowIllegalArgumentException_whenPropertyPriceIsNonPositive() {
            // Arrange
            CalculationRequest zeroPrice = new CalculationRequest(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    TestConstants.TERM_MONTHS,
                    TestConstants.ANNUAL_RATE,
                    PaymentType.ANNUITY,
                    LocalDate.now(),
                    List.of()
            );

            // Act & Assert
            assertThatThrownBy(() -> validator.validate(zeroPrice))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Property price must be greater than zero");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when down payment is negative")
        void validate_shouldThrowIllegalArgumentException_whenDownPaymentIsNegative() {
            // Arrange
            CalculationRequest request = new CalculationRequest(
                    TestConstants.PROPERTY_PRICE,
                    new BigDecimal("-100.00"),
                    TestConstants.TERM_MONTHS,
                    TestConstants.ANNUAL_RATE,
                    PaymentType.ANNUITY,
                    LocalDate.now(),
                    List.of()
            );

            // Act & Assert
            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Down payment cannot be negative");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when down payment is equal to property price")
        void validate_shouldThrowIllegalArgumentException_whenDownPaymentEqualsPropertyPrice() {
            // Arrange
            CalculationRequest request = new CalculationRequest(
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.TERM_MONTHS,
                    TestConstants.ANNUAL_RATE,
                    PaymentType.ANNUITY,
                    LocalDate.now(),
                    List.of()
            );

            // Act & Assert
            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Down payment cannot be greater than or equal to property price");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when down payment exceeds property price")
        void validate_shouldThrowIllegalArgumentException_whenDownPaymentExceedsPropertyPrice() {
            // Arrange
            CalculationRequest request = new CalculationRequest(
                    TestConstants.PROPERTY_PRICE,
                    new BigDecimal("6000000.00"),
                    TestConstants.TERM_MONTHS,
                    TestConstants.ANNUAL_RATE,
                    PaymentType.ANNUITY,
                    LocalDate.now(),
                    List.of()
            );

            // Act & Assert
            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Down payment cannot be greater than or equal to property price");
        }

        @ParameterizedTest(name = "Term months: {0}")
        @ValueSource(ints = {0, -1, -12})
        @DisplayName("Should throw IllegalArgumentException when term months is zero or negative")
        void validate_shouldThrowIllegalArgumentException_whenTermMonthsIsNonPositive(int invalidTerm) {
            // Arrange
            CalculationRequest request = new CalculationRequest(
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.DOWN_PAYMENT,
                    invalidTerm,
                    TestConstants.ANNUAL_RATE,
                    PaymentType.ANNUITY,
                    LocalDate.now(),
                    List.of()
            );

            // Act & Assert
            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Loan term must be at least 1 month");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when annual interest rate is negative")
        void validate_shouldThrowIllegalArgumentException_whenInterestRateIsNegative() {
            // Arrange
            CalculationRequest request = new CalculationRequest(
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.DOWN_PAYMENT,
                    TestConstants.TERM_MONTHS,
                    new BigDecimal("-1.00"),
                    PaymentType.ANNUITY,
                    LocalDate.now(),
                    List.of()
            );

            // Act & Assert
            assertThatThrownBy(() -> validator.validate(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Annual interest rate cannot be negative");
        }
    }
}
