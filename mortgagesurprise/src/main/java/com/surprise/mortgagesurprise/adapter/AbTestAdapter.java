package com.surprise.mortgagesurprise.adapter;

import com.surprise.mortgagesurprise.dto.abtest.AbTestAssignmentResponse;
import com.surprise.mortgagesurprise.dto.abtest.AbTestStatsResponse;
import com.surprise.mortgagesurprise.security.CustomUserDetails;

public interface AbTestAdapter {
    AbTestAssignmentResponse getOrCreateVariant(CustomUserDetails userDetails, String experimentName);

    AbTestStatsResponse getStats(String experimentName);
}
