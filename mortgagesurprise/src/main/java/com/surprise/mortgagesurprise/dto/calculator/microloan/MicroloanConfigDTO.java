package com.surprise.mortgagesurprise.dto.calculator.microloan;

import com.surprise.mortgagesurprise.model.entity.MicroloanConfig;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MicroloanConfigDTO(
        @NotNull(message = "Bank interest fixed surcharge is required")
        @DecimalMin(value = "0.00", message = "Bank interest must be non-negative")
        BigDecimal bankInterest
) {
    public static MicroloanConfigDTO from(MicroloanConfig config) {
        return new MicroloanConfigDTO(config.getBankInterest());
    }
}
