package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculationRequestValidatorTest {

    private CalculationRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CalculationRequestValidator();
    }

    @Test
    @DisplayName("Valid request passes validation")
    void testValidRequest() {
        CalculationRequest request = new CalculationRequest(
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                240,
                new BigDecimal("12.00"),
                PaymentType.ANNUITY,
                LocalDate.now(),
                List.of()
        );

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("Null request throws IllegalArgumentException")
    void testNullRequest() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(null));
    }

    @Test
    @DisplayName("Invalid property price throws IllegalArgumentException")
    void testInvalidPropertyPrice() {
        CalculationRequest nullPrice = new CalculationRequest(
                null,
                new BigDecimal("1000000.00"),
                240,
                new BigDecimal("12.00"),
                PaymentType.ANNUITY,
                LocalDate.now(),
                List.of()
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(nullPrice));

        CalculationRequest zeroPrice = new CalculationRequest(
                BigDecimal.ZERO,
                new BigDecimal("1000000.00"),
                240,
                new BigDecimal("12.00"),
                PaymentType.ANNUITY,
                LocalDate.now(),
                List.of()
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(zeroPrice));
    }

    @Test
    @DisplayName("Negative down payment throws IllegalArgumentException")
    void testNegativeDownPayment() {
        CalculationRequest request = new CalculationRequest(
                new BigDecimal("5000000.00"),
                new BigDecimal("-100.00"),
                240,
                new BigDecimal("12.00"),
                PaymentType.ANNUITY,
                LocalDate.now(),
                List.of()
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(request));
    }

    @Test
    @DisplayName("Down payment greater than or equal to property price throws IllegalArgumentException")
    void testDownPaymentExceedsPrice() {
        CalculationRequest equal = new CalculationRequest(
                new BigDecimal("5000000.00"),
                new BigDecimal("5000000.00"),
                240,
                new BigDecimal("12.00"),
                PaymentType.ANNUITY,
                LocalDate.now(),
                List.of()
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(equal));

        CalculationRequest greater = new CalculationRequest(
                new BigDecimal("5000000.00"),
                new BigDecimal("6000000.00"),
                240,
                new BigDecimal("12.00"),
                PaymentType.ANNUITY,
                LocalDate.now(),
                List.of()
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(greater));
    }

    @Test
    @DisplayName("Term months <= 0 throws IllegalArgumentException")
    void testInvalidTermMonths() {
        CalculationRequest request = new CalculationRequest(
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                0,
                new BigDecimal("12.00"),
                PaymentType.ANNUITY,
                LocalDate.now(),
                List.of()
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(request));
    }

    @Test
    @DisplayName("Negative annual interest rate throws IllegalArgumentException")
    void testNegativeInterestRate() {
        CalculationRequest request = new CalculationRequest(
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                240,
                new BigDecimal("-1.00"),
                PaymentType.ANNUITY,
                LocalDate.now(),
                List.of()
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(request));
    }
}
