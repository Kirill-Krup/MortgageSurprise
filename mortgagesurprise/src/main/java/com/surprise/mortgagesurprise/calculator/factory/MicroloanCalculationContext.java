package com.surprise.mortgagesurprise.calculator.factory;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.MicroloanType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MicroloanCalculationContext(
        MicroloanCalculationRequest request,
        BigDecimal loanAmount,
        int termDays,
        MicroloanType microloanType,
        String purpose,
        BigDecimal baseDailyRate,
        BigDecimal vanitySurchargeRate,
        BigDecimal effectiveDailyRate,
        BigDecimal effectiveDailyRateFraction,
        BigDecimal annualEquivalentRate,
        LocalDate startDate,
        LocalDate dueDate,
        Currency currency
) {}
