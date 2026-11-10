package com.surprise.mortgagesurprise.dto.scenario;

import com.surprise.mortgagesurprise.dto.calculator.EarlyRepaymentRequest;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record SaveScenarioRequest(
        @NotBlank(message = "Title is required")
        String title,

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

        @NotNull(message = "Payment type is required")
        PaymentType paymentType,

        List<EarlyRepaymentRequest> earlyRepayments
) {}
