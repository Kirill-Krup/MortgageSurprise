package com.surprise.mortgagesurprise.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminLoanApplicationDto(
        String id,
        UUID userId,
        String userEmail,
        String fullName,
        BigDecimal amount,
        Integer termDays,
        String purpose,
        String microloanType,
        BigDecimal totalRepayment,
        String currency,
        Double scoring,
        String status,
        Instant createdAt,
        Integer age,
        String country,
        String city,
        String gender,
        String passportSeriesNumber
) {}
