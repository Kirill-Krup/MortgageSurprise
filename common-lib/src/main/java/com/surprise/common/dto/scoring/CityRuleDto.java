package com.surprise.common.dto.scoring;

public record CityRuleDto(
        Long id,
        String cityName,
        RuleType ruleType,
        Float creditScore
) {}
