package com.surprise.mortgagesurprise.dto.calculator;

import java.math.BigDecimal;
import java.util.List;

public record CalculationResponse(
        String scheduleId,
        BigDecimal propertyPrice,
        BigDecimal downPayment,
        BigDecimal loanAmount,
        int termMonths,
        int actualTermMonths,
        BigDecimal annualInterestRate,
        String paymentType,
        BigDecimal monthlyPayment,
        BigDecimal minMonthlyPayment,
        BigDecimal maxMonthlyPayment,
        BigDecimal totalInterest,
        BigDecimal totalPayment,
        List<PaymentScheduleItem> schedule
) {}
