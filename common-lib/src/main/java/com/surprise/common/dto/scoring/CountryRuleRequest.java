package com.surprise.common.dto.scoring;

public record CountryRuleRequest(
        String countryCode,
        RuleType ruleType,
        Float creditScore
) {}
