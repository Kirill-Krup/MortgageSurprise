package com.bank.simulator.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record ErrorResponse(
        int status,
        String error,
        String message,
        List<String> details,
        Instant timestamp
) {}
