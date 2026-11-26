package com.surprise.common.dto.simulator;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record TrustEventResponse(
        UUID id,
        UUID subjectId,
        String eventKey,
        TrustEventType eventType,
        int delta,
        int scoreAfter,
        String source,
        Instant occurredAt,
        Instant createdAt
) {}
