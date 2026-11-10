package com.surprise.mortgagesurprise.dto.calculator;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentScheduleItem(
        int monthNumber,
        LocalDate paymentDate,
        BigDecimal openingBalance,
        BigDecimal principalPayment,
        BigDecimal interestPayment,
        BigDecimal earlyPayment,
        BigDecimal totalMonthlyPayment,
        BigDecimal closingBalance
) {}
