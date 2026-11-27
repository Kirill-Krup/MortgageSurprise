package com.surprise.mortgagesurprise.dto.calculator.microloan;

import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.MicroloanType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MicroloanCalculationResponse(
        String scheduleId,
        BigDecimal loanAmount,
        int termDays,
        MicroloanType microloanType,
        String purpose,
        BigDecimal baseDailyRate,
        BigDecimal vanitySurchargeRate,
        BigDecimal vanitySurchargeAmount,
        BigDecimal effectiveDailyRate,
        BigDecimal annualEquivalentRate,
        BigDecimal totalInterest,
        BigDecimal totalRepayment,
        LocalDate startDate,
        LocalDate dueDate,
        Currency currency,
        List<DailyScheduleItem> dailySchedule
) {}
