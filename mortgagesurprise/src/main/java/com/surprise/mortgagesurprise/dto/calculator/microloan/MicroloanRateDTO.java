package com.surprise.mortgagesurprise.dto.calculator.microloan;

import com.surprise.mortgagesurprise.model.entity.MicroloanRate;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MicroloanRateDTO(
        Integer id,

        @NotBlank(message = "Rate name is required")
        @Size(max = 50, message = "Rate name must not exceed 50 characters")
        String name,

        @NotNull(message = "Base daily rate is required")
        @DecimalMin(value = "0.01", message = "Base rate must be positive")
        @DecimalMax(value = "10.00", message = "Base rate cannot exceed 10%")
        BigDecimal baseDailyRate,

        Integer minDayRequired,
        Integer maxDayRequired
) {
    public MicroloanRateDTO(String name, BigDecimal baseDailyRate, Integer minDayRequired, Integer maxDayRequired) {
        this(null, name, baseDailyRate, minDayRequired, maxDayRequired);
    }

    public static MicroloanRateDTO from(MicroloanRate rate) {
        if (rate == null) return null;
        return new MicroloanRateDTO(
                rate.getId(),
                rate.getName(),
                rate.getBaseDailyRate(),
                rate.getMinDayRequired(),
                rate.getMaxDayRequired()
        );
    }
}
