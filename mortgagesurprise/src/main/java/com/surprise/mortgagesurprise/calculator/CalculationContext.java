package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Context containing validated and precomputed parameters required for schedule calculation.
 */
public record CalculationContext(
        CalculationRequest request,
        BigDecimal propertyPrice,
        BigDecimal downPayment,
        BigDecimal loanAmount,
        int termMonths,
        BigDecimal annualRate,
        BigDecimal monthlyRate,
        LocalDate startDate
) {}
