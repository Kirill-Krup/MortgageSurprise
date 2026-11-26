package com.surprise.common.dto.scoring;

public record CityRuleRequest(
        String cityName,
        RuleType ruleType,
        Float creditScore
) {}
