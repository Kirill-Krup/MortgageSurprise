package com.bank.simulator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record HardBlockRequest(
        @NotBlank(message = "Block reason is required")
        @Size(max = 255, message = "Block reason must not exceed 255 characters")
        String reason
) {}
