package com.surprise.mortgagesurprise.dto.abtest;

import java.util.List;

public record CreateAbTestVariantRequest(String experimentName, Integer version,
                                         String variantName, Integer variantProbability,
                                         List<UpdateVariantsPercent> newVariants) {
}
