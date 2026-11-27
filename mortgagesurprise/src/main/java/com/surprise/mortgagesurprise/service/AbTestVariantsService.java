package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.abtest.CreateAbTestVariantRequest;
import com.surprise.mortgagesurprise.dto.abtest.ExperimentConfigDTO;
import com.surprise.mortgagesurprise.dto.abtest.UpdateAbTestVariantRequest;
import com.surprise.mortgagesurprise.model.entity.AbTestVariant;

import java.util.List;
import java.util.Set;

public interface AbTestVariantsService {
    Set<AbTestVariant> findAllByExperimentNameAndVersion(String experimentName, Integer version);

    AbTestVariant save(CreateAbTestVariantRequest request);

    Set<AbTestVariant> updateAll(UpdateAbTestVariantRequest request);

    List<ExperimentConfigDTO> getAllExperiments();

    ExperimentConfigDTO getExperiment(String id);

    ExperimentConfigDTO saveExperiment(ExperimentConfigDTO dto);

    void deleteExperiment(String id);
}