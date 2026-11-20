package com.surprise.mortgagesurprise.adapter.impl;

import com.surprise.mortgagesurprise.adapter.AbTestVariantsAdapter;
import com.surprise.mortgagesurprise.dto.abtest.AbTestVariantResponse;
import com.surprise.mortgagesurprise.dto.abtest.CreateAbTestVariantRequest;
import com.surprise.mortgagesurprise.dto.abtest.UpdateAbTestVariantRequest;
import com.surprise.mortgagesurprise.service.AbTestVariantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AbTestVariantsAdapterImpl implements AbTestVariantsAdapter {

    private final AbTestVariantsService abTestVariantsService;

    @Override
    public Set<AbTestVariantResponse> findAllByExperimentNameAndVersion(String experimentName, Integer version) {
        return abTestVariantsService.findAllByExperimentNameAndVersion(experimentName, version)
                .stream()
                .map(AbTestVariantResponse::from)
                .collect(Collectors.toSet());
    }

    @Override
    public AbTestVariantResponse save(CreateAbTestVariantRequest request) {
        return AbTestVariantResponse.from(abTestVariantsService.save(request));
    }

    @Override
    public Set<AbTestVariantResponse> updateAll(UpdateAbTestVariantRequest request) {
        return abTestVariantsService.updateAll(request)
                .stream()
                .map(AbTestVariantResponse::from)
                .collect(Collectors.toSet());
    }
}