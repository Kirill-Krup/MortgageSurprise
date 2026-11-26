package com.surprise.common.dto.scoring;

public record AgeRuleDto(
        Integer id,
        Integer minAge,
        Integer maxAge,
        RuleType ruleType,
        Float creditScore
) {}
