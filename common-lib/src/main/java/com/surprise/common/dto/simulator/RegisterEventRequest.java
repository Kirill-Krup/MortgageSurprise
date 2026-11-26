package com.surprise.common.dto.simulator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record RegisterEventRequest(
        @NotBlank(message = "eventKey is required for idempotency")
        String eventKey,

        @NotBlank(message = "documentHash is required")
        String documentHash,

        @NotNull(message = "eventType is required")
        TrustEventType eventType,

        /**
         * Опциональная дельта; если не указана, вычисляется из конфигурации правил сервиса.
         */
        Integer delta,

        @NotBlank(message = "source is required")
        String source,

        Instant occurredAt,

        Map<String, Object> details
) {}
