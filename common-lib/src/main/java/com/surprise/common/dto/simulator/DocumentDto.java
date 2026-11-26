package com.surprise.common.dto.simulator;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record DocumentDto(
        UUID id,
        DocumentType documentType,
        String documentHash,
        Instant createdAt
) {}
