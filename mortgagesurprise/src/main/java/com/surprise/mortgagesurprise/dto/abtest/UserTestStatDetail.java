package com.surprise.mortgagesurprise.dto.abtest;

import java.time.Instant;
import java.util.UUID;

public record UserTestStatDetail(
        UUID userId,
        String userEmail,
        String color,
        long clicksCount,
        Instant assignedAt
) {}
