package com.surprise.mortgagesurprise.dto.calculator.microloan;

import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.MicroloanType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MicroloanCalculationRequest(
        @NotNull(message = "Loan amount is required")
        @DecimalMin(value = "1000.00", message = "Loan amount must be at least 1,000")
        @DecimalMax(value = "100000.00", message = "Loan amount cannot exceed 100,000")
        BigDecimal amount,

        @NotNull(message = "Loan term in days is required")
        @Min(value = 7, message = "Loan term must be at least 7 days")
        @Max(value = 30, message = "Loan term cannot exceed 30 days")
        Integer termDays,

        MicroloanType type,

        String purpose,

        BigDecimal dailyInterestRate,

        LocalDate startDate,

        Currency currency
) {
    public MicroloanCalculationRequest {
        if (type == null) {
            type = MicroloanType.BASIC_NEEDS;
        }
        if (currency == null) {
            currency = Currency.RUB;
        }
        if (startDate == null) {
            startDate = LocalDate.now();
        }
    }

    public MicroloanCalculationRequest(BigDecimal amount, Integer termDays, BigDecimal dailyInterestRate) {
        this(amount, termDays, MicroloanType.BASIC_NEEDS, null, dailyInterestRate, LocalDate.now(), Currency.RUB);
    }

    public MicroloanCalculationRequest(BigDecimal amount, Integer termDays, MicroloanType type, String purpose) {
        this(amount, termDays, type, purpose, null, LocalDate.now(), Currency.RUB);
    }
}
