package com.surprise.common.dto.scoring;

public record AgeRuleRequest(
        int minAge,
        int maxAge,
        RuleType ruleType,
        Float creditScore
) {}
