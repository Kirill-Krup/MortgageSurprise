package com.surprise.mortgagesurprise.dto.calculator.microloan;

import com.surprise.mortgagesurprise.model.entity.MicroloanConfig;
import com.surprise.mortgagesurprise.model.enums.Currency;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MicroloanConfigDTO(
        @NotNull(message = "Base daily rate is required")
        @DecimalMin(value = "0.01", message = "Base rate must be positive")
        @DecimalMax(value = "2.00", message = "Base rate cannot exceed 2%")
        BigDecimal bankInterest
) {
    public static MicroloanConfigDTO from(MicroloanConfig config) {
        return new MicroloanConfigDTO(config.getBankInterest());
    }
}