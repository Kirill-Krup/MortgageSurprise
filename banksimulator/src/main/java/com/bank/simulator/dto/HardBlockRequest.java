package com.bank.simulator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record HardBlockRequest(
        @NotBlank(message = "Block reason is required")
        String reason
) {}
