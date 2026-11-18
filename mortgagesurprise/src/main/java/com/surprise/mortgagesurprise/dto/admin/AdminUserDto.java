package com.surprise.mortgagesurprise.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminUserDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean enabled,
        boolean emailVerified,
        Instant createdAt,
        int activeLoansCount,
        BigDecimal totalDebt,
        BigDecimal remainingDebt
) {
}
