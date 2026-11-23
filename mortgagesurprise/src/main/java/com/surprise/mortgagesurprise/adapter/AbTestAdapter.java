package com.surprise.mortgagesurprise.adapter;

import com.surprise.mortgagesurprise.dto.abtest.AbTestAssignmentResponse;
import com.surprise.mortgagesurprise.dto.abtest.AbTestStatsResponse;
import com.surprise.mortgagesurprise.dto.abtest.ExperimentConfigDTO;
import com.surprise.mortgagesurprise.security.CustomUserDetails;

import java.util.List;

public interface AbTestAdapter {
    AbTestAssignmentResponse getOrCreateVariant(CustomUserDetails userDetails, String experimentName);

    AbTestStatsResponse getStats(String experimentName);

    List<ExperimentConfigDTO> getAllExperiments();

    ExperimentConfigDTO getExperiment(String id);

    ExperimentConfigDTO saveExperiment(ExperimentConfigDTO dto);

    void deleteExperiment(String id);
}
