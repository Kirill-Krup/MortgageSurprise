package com.surprise.mortgagesurprise.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminLoanMonitoringDto(
        String id,
        UUID userId,
        String borrowerEmail,
        String borrowerName,
        String loanType,
        String productTitle,
        String purpose,
        BigDecimal loanAmount,
        BigDecimal totalRepayment,
        BigDecimal interestRate,
        String term,
        int termUnits,
        String currency,
        Instant startDate,
        int elapsedPeriods,
        int remainingPeriods,
        BigDecimal paidAmount,
        BigDecimal principalPaid,
        BigDecimal remainingBalance,
        double progressPercent,
        BigDecimal regularPayment,
        String status,
        String nextPaymentDate,
        BigDecimal nextPaymentAmount,
        String scheduleId
) {
}
