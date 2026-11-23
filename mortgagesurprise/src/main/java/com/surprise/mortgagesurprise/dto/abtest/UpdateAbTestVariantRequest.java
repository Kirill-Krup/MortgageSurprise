package com.surprise.mortgagesurprise.dto.abtest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateAbTestVariantRequest(
        @NotBlank @Size(max = 100) String experimentName,
        @NotNull @Min(0) Integer version,
        @NotEmpty @Valid List<UpdateVariantsPercent> newVariants) {
}
