package com.surprise.mortgagesurprise.dto.audit;

import com.surprise.mortgagesurprise.model.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MicroloanApplicationRequest(
        @NotNull(message = "Loan amount is required")
        @DecimalMin(value = "1.00", message = "Loan amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Term in days is required")
        @Min(value = 1, message = "Term must be at least 1 day")
        Integer termDays,

        String purpose,
        String microloanType,
        BigDecimal totalRepayment,
        String scheduleId,
        String applicationId,
        String applicantFullName,
        Currency currency
) {}
