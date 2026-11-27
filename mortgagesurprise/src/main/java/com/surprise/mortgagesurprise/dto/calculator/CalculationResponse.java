package com.surprise.mortgagesurprise.dto.calculator;

import com.surprise.mortgagesurprise.model.enums.Currency;

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
        Currency currency,
        BigDecimal monthlyPayment,
        BigDecimal minMonthlyPayment,
        BigDecimal maxMonthlyPayment,
        BigDecimal totalInterest,
        BigDecimal totalPayment,
        List<PaymentScheduleItem> schedule
) {
    public CalculationResponse(String scheduleId, BigDecimal propertyPrice, BigDecimal downPayment,
                               BigDecimal loanAmount, int termMonths, int actualTermMonths,
                               BigDecimal annualInterestRate, String paymentType,
                               BigDecimal monthlyPayment, BigDecimal minMonthlyPayment,
                               BigDecimal maxMonthlyPayment, BigDecimal totalInterest,
                               BigDecimal totalPayment, List<PaymentScheduleItem> schedule) {
        this(scheduleId, propertyPrice, downPayment, loanAmount, termMonths, actualTermMonths,
                annualInterestRate, paymentType, Currency.RUB, monthlyPayment, minMonthlyPayment,
                maxMonthlyPayment, totalInterest, totalPayment, schedule);
    }
}
