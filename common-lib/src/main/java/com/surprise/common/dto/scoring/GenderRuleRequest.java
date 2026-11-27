package com.surprise.common.dto.scoring;

public record GenderRuleRequest(
        String name,
        RuleType ruleType,
        Float creditScore
) {}
