package com.surprise.mortgagesurprise.dto.abtest;

import java.util.List;

public record AbTestStatsResponse(
        String experimentName,
        long totalAssignedUsers,
        long totalClicks,
        List<VariantStat> variants,
        List<UserTestStatDetail> userDetails
) {}
