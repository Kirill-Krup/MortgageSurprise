package com.surprise.mortgagesurprise.dto.abtest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateAbTestVariantRequest(
        @NotBlank @Size(max = 100) String experimentName,
        @NotNull @Min(0) Integer version,
        @NotBlank @Size(max = 50) String variantName,
        @NotNull @Min(0) @Max(100) Integer variantProbability,
        @Valid List<UpdateVariantsPercent> newVariants) {
}
