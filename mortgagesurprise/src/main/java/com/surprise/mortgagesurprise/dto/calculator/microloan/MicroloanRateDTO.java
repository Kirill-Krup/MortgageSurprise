package com.surprise.mortgagesurprise.dto.calculator.microloan;

import com.surprise.mortgagesurprise.model.entity.MicroloanRate;

import java.math.BigDecimal;

public record MicroloanRateDTO(String name,
                               BigDecimal baseDailyRate,
                               Integer minDayRequired,
                               Integer maxDayRequired) {
    public static MicroloanRateDTO from(MicroloanRate rate) {
        return new MicroloanRateDTO(rate.getName(), rate.getBaseDailyRate(), rate.getMinDayRequired(),
                rate.getMaxDayRequired());
    }
}