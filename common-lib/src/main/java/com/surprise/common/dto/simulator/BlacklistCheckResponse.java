package com.surprise.common.dto.simulator;

import lombok.Builder;

import java.util.List;

@Builder
public record BlacklistCheckResponse(
        boolean blacklisted,
        int trustScore,
        RiskLevel riskLevel,
        boolean hardBlock,
        List<String> reasons
) {}
