package com.surprise.mortgagesurprise.adapter.impl;

import com.surprise.mortgagesurprise.adapter.AbTestAdapter;
import com.surprise.mortgagesurprise.dto.abtest.AbTestAssignmentResponse;
import com.surprise.mortgagesurprise.dto.abtest.AbTestStatsResponse;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.AbTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.surprise.mortgagesurprise.util.CredentialValidator.isIdExists;

@Service
@RequiredArgsConstructor
public class AbTestAdapterImpl implements AbTestAdapter {

    private final AbTestService abTestService;

    @Override
    public AbTestAssignmentResponse getOrCreateVariant(CustomUserDetails userDetails, String experimentName) {
        if (experimentName == null || experimentName.isBlank()) {
            throw new IllegalArgumentException("Experiment name must not be blank");
        }
        if (!isIdExists(userDetails)) {
            String colourName = "blue";
            return new AbTestAssignmentResponse(null, experimentName.trim(), colourName);
        }
        return AbTestAssignmentResponse.from(abTestService.getOrCreateVariant(userDetails.getId(),
                experimentName));
    }

    @Override
    public AbTestStatsResponse getStats(String experimentName) {
        return abTestService.getStats(experimentName);
    }
}