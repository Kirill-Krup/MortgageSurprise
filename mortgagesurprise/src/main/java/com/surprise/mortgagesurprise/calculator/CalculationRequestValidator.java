package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Single responsibility: Validate mortgage calculation request inputs against financial business rules.
 */
@Component
public class CalculationRequestValidator {

    public void validate(CalculationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Calculation request cannot be null");
        }
        if (request.propertyPrice() == null || request.propertyPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Property price must be greater than zero");
        }
        if (request.downPayment() == null || request.downPayment().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Down payment cannot be negative");
        }
        if (request.downPayment().compareTo(request.propertyPrice()) >= 0) {
            throw new IllegalArgumentException("Down payment cannot be greater than or equal to property price");
        }
        if (request.termMonths() == null || request.termMonths() <= 0) {
            throw new IllegalArgumentException("Loan term must be at least 1 month");
        }
        if (request.annualInterestRate() == null || request.annualInterestRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Annual interest rate cannot be negative");
        }
    }
}
