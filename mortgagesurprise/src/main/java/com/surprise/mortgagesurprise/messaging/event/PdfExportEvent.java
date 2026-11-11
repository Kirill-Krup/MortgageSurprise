package com.surprise.mortgagesurprise.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record PdfExportEvent(
        UUID userId,
        String userEmail,
        UUID scenarioId,
        String scenarioTitle,
        String mongoScheduleId,
        Instant requestedAt
) {}