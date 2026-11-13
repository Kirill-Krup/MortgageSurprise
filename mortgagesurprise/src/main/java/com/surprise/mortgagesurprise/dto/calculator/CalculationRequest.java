package com.surprise.mortgagesurprise.dto.calculator;

import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CalculationRequest(
        @NotNull(message = "Property price is required")
        @DecimalMin(value = "1.00", message = "Property price must be positive")
        BigDecimal propertyPrice,

        @NotNull(message = "Down payment is required")
        @DecimalMin(value = "0.00", message = "Down payment cannot be negative")
        BigDecimal downPayment,

        @NotNull(message = "Loan term in months is required")
        @Min(value = 1, message = "Loan term must be at least 1 month")
        Integer termMonths,

        @NotNull(message = "Annual interest rate is required")
        @DecimalMin(value = "0.00", message = "Annual interest rate cannot be negative")
        BigDecimal annualInterestRate,

        @NotNull(message = "Payment type is required (ANNUITY or DIFFERENTIATED)")
        PaymentType paymentType,

        Currency currency,

        LocalDate startDate,

        @Valid
        List<EarlyRepaymentRequest> earlyRepayments
) {
    public CalculationRequest {
        if (currency == null) {
            currency = Currency.RUB;
        }
        if (earlyRepayments == null) {
            earlyRepayments = List.of();
        }
        if (startDate == null) {
            startDate = LocalDate.now();
        }
    }

    public CalculationRequest(BigDecimal propertyPrice, BigDecimal downPayment, Integer termMonths,
                              BigDecimal annualInterestRate, PaymentType paymentType,
                              LocalDate startDate, List<EarlyRepaymentRequest> earlyRepayments) {
        this(propertyPrice, downPayment, termMonths, annualInterestRate, paymentType, Currency.RUB, startDate, earlyRepayments);
    }
}
