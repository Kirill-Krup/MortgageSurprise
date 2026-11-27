package com.surprise.mortgagesurprise.dto.abtest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateVariantsPercent(
        @NotNull Integer variantId,
        @NotNull @Min(0) @Max(100) Integer percent) {
}
