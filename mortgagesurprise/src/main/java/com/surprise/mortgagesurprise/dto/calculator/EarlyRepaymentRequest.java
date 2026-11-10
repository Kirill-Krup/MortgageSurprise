package com.surprise.mortgagesurprise.dto.calculator;

import com.surprise.mortgagesurprise.model.enums.EarlyRepaymentType;
import com.surprise.mortgagesurprise.model.enums.RepaymentStrategy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EarlyRepaymentRequest(
        @NotNull(message = "Start month is required")
        @Min(value = 1, message = "Start month must be at least 1")
        Integer startMonth,

        @Min(value = 1, message = "End month must be at least 1")
        Integer endMonth,

        @NotNull(message = "Early repayment amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotNull(message = "Repayment strategy is required")
        RepaymentStrategy strategy,

        @NotNull(message = "Early repayment type is required")
        EarlyRepaymentType type
) {}
