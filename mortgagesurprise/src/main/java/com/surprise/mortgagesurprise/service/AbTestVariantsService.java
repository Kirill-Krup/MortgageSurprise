package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.abtest.CreateAbTestVariantRequest;
import com.surprise.mortgagesurprise.dto.abtest.UpdateAbTestVariantRequest;
import com.surprise.mortgagesurprise.model.entity.AbTestVariant;

import java.util.Set;

public interface AbTestVariantsService {
    Set<AbTestVariant> findAllByExperimentNameAndVersion(String experimentName, Integer version);

    AbTestVariant save(CreateAbTestVariantRequest request);

    Set<AbTestVariant> updateAll(UpdateAbTestVariantRequest request);
}