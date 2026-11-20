package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.abtest.AbTestStatsResponse;
import com.surprise.mortgagesurprise.model.entity.UserAbTestVariant;

import java.util.UUID;

public interface AbTestService {

    default UserAbTestVariant getOrCreateVariant(UUID userId, String experimentName) {
        return getOrCreateVariant(userId, experimentName, null);
    }

    UserAbTestVariant getOrCreateVariant(UUID userId, String experimentName, String preferredColor);

    AbTestStatsResponse getStats(String experimentName);
}
