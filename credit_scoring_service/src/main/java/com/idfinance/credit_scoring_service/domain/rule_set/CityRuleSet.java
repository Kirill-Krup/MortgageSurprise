package com.idfinance.credit_scoring_service.domain.rule_set;

import com.idfinance.credit_scoring_service.model.CityRule;
import com.idfinance.credit_scoring_service.model.RuleType;

public record CityRuleSet(Long id, String cityName, RuleType ruleType, Float creditScore)
        implements RuleSet<String>, Identifiable<Long> {
    @Override
    public boolean apply(String s) {
        if (s == null || cityName == null || ruleType == null) {
            return false;
        }
        return switch (ruleType) {
            case INCLUSIVE -> s.trim().equalsIgnoreCase(cityName.trim());
            case EXCLUSIVE -> !s.trim().equalsIgnoreCase(cityName.trim());
        };
    }

    public static CityRuleSet from(CityRule cityRule) {
        return new CityRuleSet(cityRule.getId(), cityRule.getCityName(), cityRule.getRuleType(),
                cityRule.getCreditScore());
    }

    @Override
    public Long getId() {
        return id;
    }
}