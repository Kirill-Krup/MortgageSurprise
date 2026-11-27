package com.surprise.mortgagesurprise.dto.calculator.microloan;

import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.MicroloanType;
import jakarta.validation.constraints.*;

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

        @NotNull
        @Size(min = 3)
        String rateName,

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

    public MicroloanCalculationRequest(BigDecimal amount, Integer termDays, String rateName, BigDecimal dailyInterestRate) {
        this(amount, termDays, rateName, MicroloanType.BASIC_NEEDS, null, dailyInterestRate, LocalDate.now(), Currency.RUB);
    }

    public MicroloanCalculationRequest(BigDecimal amount, Integer termDays, String rateName, MicroloanType type, String purpose) {
        this(amount, termDays, rateName, type, purpose, null, LocalDate.now(), Currency.RUB);
    }
}