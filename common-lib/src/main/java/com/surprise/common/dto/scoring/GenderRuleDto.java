package com.surprise.common.dto.scoring;

public record GenderRuleDto(
        Integer id,
        String gender,
        RuleType ruleType,
        Float creditScore
) {}
