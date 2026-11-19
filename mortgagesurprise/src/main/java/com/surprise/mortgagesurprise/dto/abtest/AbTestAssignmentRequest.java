package com.surprise.mortgagesurprise.dto.abtest;

import jakarta.validation.constraints.NotBlank;

public record AbTestAssignmentRequest(
        @NotBlank(message = "Experiment name must not be blank")
        String experimentName,

        @NotBlank(message = "Color must not be blank")
        String color
) {}
