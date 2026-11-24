package com.surprise.mortgagesurprise.adapter.impl;

import com.surprise.mortgagesurprise.adapter.AbTestAdapter;
import com.surprise.mortgagesurprise.dto.abtest.AbTestAssignmentResponse;
import com.surprise.mortgagesurprise.dto.abtest.AbTestStatsResponse;
import com.surprise.mortgagesurprise.dto.abtest.ExperimentConfigDTO;
import com.surprise.mortgagesurprise.repository.jpa.AbTestVariantsRepository;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.AbTestService;
import com.surprise.mortgagesurprise.service.AbTestVariantsService;
import com.surprise.mortgagesurprise.util.AbTestVariantUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.surprise.mortgagesurprise.util.CredentialValidator.isIdExists;

@Service
@RequiredArgsConstructor
public class AbTestAdapterImpl implements AbTestAdapter {

    private final AbTestService abTestService;
    private final AbTestVariantsService abTestVariantsService;
    private final AbTestVariantsRepository abTestVariantsRepository;

    @Override
    public AbTestAssignmentResponse getOrCreateVariant(CustomUserDetails userDetails, String experimentName) {
        if (experimentName == null || experimentName.isBlank()) {
            throw new IllegalArgumentException("Experiment name must not be blank");
        }
        var expName = experimentName.trim();
        if (!isIdExists(userDetails)) {
            String colourName = abTestVariantsRepository.findLatestVersion(expName)
                    .flatMap(v -> AbTestVariantUtil.pickVariant(
                            UUID.randomUUID().toString(),
                            expName,
                            abTestVariantsRepository.findAllByExperimentNameAndVersion(expName, v)))
                    .map(com.surprise.mortgagesurprise.model.entity.AbTestVariant::getVariantName)
                    .orElse("blue");
            return new AbTestAssignmentResponse(null, expName, colourName);
        }
        return AbTestAssignmentResponse.from(abTestService.getOrCreateVariant(userDetails.getId(), expName));
    }

    @Override
    public AbTestStatsResponse getStats(String experimentName) {
        return abTestService.getStats(experimentName);
    }

    @Override
    public List<ExperimentConfigDTO> getAllExperiments() {
        return abTestVariantsService.getAllExperiments();
    }

    @Override
    public ExperimentConfigDTO getExperiment(String id) {
        return abTestVariantsService.getExperiment(id);
    }

    @Override
    public ExperimentConfigDTO saveExperiment(ExperimentConfigDTO dto) {
        return abTestVariantsService.saveExperiment(dto);
    }

    @Override
    public void deleteExperiment(String id) {
        abTestVariantsService.deleteExperiment(id);
    }
}