package com.surprise.mortgagesurprise.dto.calculator;

import com.surprise.mortgagesurprise.model.enums.PaymentType;

import java.math.BigDecimal;
import java.util.List;

public record CalculationResult(
        BigDecimal propertyPrice,
        BigDecimal downPayment,
        BigDecimal loanAmount,
        int termMonths,
        int actualTermMonths,
        BigDecimal annualInterestRate,
        PaymentType paymentType,
        BigDecimal monthlyPayment,
        BigDecimal minMonthlyPayment,
        BigDecimal maxMonthlyPayment,
        BigDecimal totalInterest,
        BigDecimal totalPayment,
        List<PaymentScheduleItem> schedule
) {}
