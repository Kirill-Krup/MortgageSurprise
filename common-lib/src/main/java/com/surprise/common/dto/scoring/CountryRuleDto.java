package com.surprise.common.dto.scoring;

public record CountryRuleDto(
        Integer id,
        String countryName,
        RuleType ruleType,
        Float creditScore
) {}
