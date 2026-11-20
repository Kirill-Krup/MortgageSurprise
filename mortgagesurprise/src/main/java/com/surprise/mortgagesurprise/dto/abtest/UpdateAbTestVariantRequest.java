package com.surprise.mortgagesurprise.dto.abtest;

import java.util.List;

public record UpdateAbTestVariantRequest(String experimentName, Integer version,
                                         List<UpdateVariantsPercent> newVariants) {
}
