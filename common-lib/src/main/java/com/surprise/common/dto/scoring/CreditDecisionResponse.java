package com.surprise.common.dto.scoring;

public record CreditDecisionResponse(
        boolean approved,
        String reason,
        float score
) {}
