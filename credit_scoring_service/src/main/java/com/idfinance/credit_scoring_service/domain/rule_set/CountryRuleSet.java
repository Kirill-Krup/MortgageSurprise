package com.idfinance.credit_scoring_service.domain.rule_set;

import com.idfinance.credit_scoring_service.model.CountryRule;
import com.idfinance.credit_scoring_service.model.RuleType;

public record CountryRuleSet(Integer id, String countryName, RuleType ruleType, Float creditScore)
        implements RuleSet<String>, Identifiable<Integer> {
    @Override
    public boolean apply(String s) {
        switch (ruleType) {
            case INCLUSIVE -> {
                return s.trim().equals(countryName);
            }
            case EXCLUSIVE -> {
                return !(s.trim().equals(countryName));
            }
        }
        return false;
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