package com.surprise.common.dto.simulator;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record SubjectResponse(
        UUID id,
        int trustScore,
        boolean hardBlock,
        String blockReason,
        RiskLevel riskLevel,
        boolean blacklisted,
        List<DocumentDto> documents,
        Instant createdAt,
        Instant updatedAt
) {}
