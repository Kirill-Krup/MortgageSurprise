package com.surprise.mortgagesurprise.adapter;

import com.surprise.mortgagesurprise.dto.abtest.AbTestVariantResponse;
import com.surprise.mortgagesurprise.dto.abtest.CreateAbTestVariantRequest;
import com.surprise.mortgagesurprise.dto.abtest.UpdateAbTestVariantRequest;

import java.util.Set;

public interface AbTestVariantsAdapter {
    Set<AbTestVariantResponse> findAllByExperimentNameAndVersion(String experimentName, Integer version);

    AbTestVariantResponse save(CreateAbTestVariantRequest request);

    Set<AbTestVariantResponse> updateAll(UpdateAbTestVariantRequest request);
}
