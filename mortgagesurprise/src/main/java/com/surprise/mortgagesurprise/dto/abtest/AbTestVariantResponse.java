package com.surprise.mortgagesurprise.dto.abtest;

import com.surprise.mortgagesurprise.model.entity.AbTestVariant;

public record AbTestVariantResponse(Integer id, String experimentName, String variantName,
                                    Integer variantProbability, Integer version) {
    public static AbTestVariantResponse from(AbTestVariant x) {
        return new AbTestVariantResponse(x.getId(), x.getExperimentName(), x.getVariantName(),
                x.getVariantProbability(), x.getVersion());
    }
}
