package com.surprise.mortgagesurprise.dto.calculator.microloan;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyScheduleItem(
        int dayNumber,
        LocalDate date,
        BigDecimal principalBalance,
        BigDecimal dailyInterest,
        BigDecimal accumulatedInterest,
        BigDecimal totalDue
) {}
