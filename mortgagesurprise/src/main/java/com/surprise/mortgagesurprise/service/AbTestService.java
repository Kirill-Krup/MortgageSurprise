package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.abtest.AbTestAssignmentResponse;
import com.surprise.mortgagesurprise.dto.abtest.AbTestStatsResponse;

import java.util.UUID;

public interface AbTestService {

    default AbTestAssignmentResponse getOrCreateVariant(UUID userId, String experimentName) {
        return getOrCreateVariant(userId, experimentName, null);
    }

    AbTestAssignmentResponse getOrCreateVariant(UUID userId, String experimentName, String preferredColor);

    AbTestStatsResponse getStats(String experimentName);
}
