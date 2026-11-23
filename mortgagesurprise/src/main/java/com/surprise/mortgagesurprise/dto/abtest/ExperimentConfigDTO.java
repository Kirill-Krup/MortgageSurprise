package com.surprise.mortgagesurprise.dto.abtest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ExperimentConfigDTO(
        @NotBlank String id,
        String name,
        String description,
        @NotEmpty @Valid List<ExperimentVariantConfigDTO> variants,
        String status,
        Boolean isCustom,
        String createdAt,
        String updatedAt
) {
}
