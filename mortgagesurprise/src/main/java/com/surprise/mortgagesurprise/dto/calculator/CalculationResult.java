package com.surprise.mortgagesurprise.dto.calculator;

import com.surprise.mortgagesurprise.model.enums.Currency;
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
        Currency currency,
        BigDecimal monthlyPayment,
        BigDecimal minMonthlyPayment,
        BigDecimal maxMonthlyPayment,
        BigDecimal totalInterest,
        BigDecimal totalPayment,
        List<PaymentScheduleItem> schedule
) {
    public CalculationResult(BigDecimal propertyPrice, BigDecimal downPayment, BigDecimal loanAmount,
                             int termMonths, int actualTermMonths, BigDecimal annualInterestRate,
                             PaymentType paymentType, BigDecimal monthlyPayment,
                             BigDecimal minMonthlyPayment, BigDecimal maxMonthlyPayment,
                             BigDecimal totalInterest, BigDecimal totalPayment,
                             List<PaymentScheduleItem> schedule) {
        this(propertyPrice, downPayment, loanAmount, termMonths, actualTermMonths, annualInterestRate,
                paymentType, Currency.RUB, monthlyPayment, minMonthlyPayment, maxMonthlyPayment,
                totalInterest, totalPayment, schedule);
    }
}
