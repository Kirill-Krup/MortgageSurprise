package com.idfinance.credit_scoring_service.domain.rule_set;

import com.idfinance.credit_scoring_service.model.CountryRule;
import com.idfinance.credit_scoring_service.model.RuleType;

public record CountryRuleSet(Integer id, String countryName, RuleType ruleType, Float creditScore)
        implements RuleSet<String>, Identifiable<Integer> {
    @Override
    public boolean apply(String s) {
        if (s == null || countryName == null || ruleType == null) {
            return false;
        }
        return switch (ruleType) {
            case INCLUSIVE -> s.trim().equalsIgnoreCase(countryName.trim());
            case EXCLUSIVE -> !s.trim().equalsIgnoreCase(countryName.trim());
        };
    }

    public static CountryRuleSet from(CountryRule countryRule) {
        return new CountryRuleSet(countryRule.getId(), countryRule.getCountryCode(),
                countryRule.getRuleType(), countryRule.getCreditScore());
    }

    @Override
    public Integer getId() {
        return id;
    }
}