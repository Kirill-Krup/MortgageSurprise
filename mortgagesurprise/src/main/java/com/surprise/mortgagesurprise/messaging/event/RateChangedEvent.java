package com.surprise.mortgagesurprise.messaging.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RateChangedEvent(
        UUID bankOfferId,
        String bankName,
        BigDecimal oldRate,
        BigDecimal newRate,
        Instant timestamp
) implements Serializable {}
