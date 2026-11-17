package com.surprise.mortgagesurprise.dto.calculator.microloan;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MicroloanConfigDTO(
        @NotNull(message = "Base daily rate is required")
        @DecimalMin(value = "0.01", message = "Base rate must be positive")
        @DecimalMax(value = "2.00", message = "Base rate cannot exceed 2%")
        BigDecimal baseDailyRate,

        @NotNull(message = "Vanity surcharge rate is required")
        @DecimalMin(value = "0.00", message = "Vanity surcharge cannot be negative")
        @DecimalMax(value = "50.00", message = "Vanity surcharge cannot exceed 50%")
        BigDecimal vanitySurchargeRate
) {}
