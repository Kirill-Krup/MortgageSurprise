package com.idfinance.credit_scoring_service.domain.rule_set;

import com.idfinance.credit_scoring_service.model.CityRule;
import com.idfinance.credit_scoring_service.model.RuleType;

public record CityRuleSet(String cityName, RuleType ruleType, Float creditScore) implements RuleSet<String> {
    @Override
    public boolean apply(String s) {
        switch (ruleType) {
            case INCLUSIVE -> {
                return s.trim().equals(cityName);
            }
            case EXCLUSIVE -> {
                return !(s.trim().equals(cityName));
            }
        }
        return false;
    }

    public static CityRuleSet from(CityRule cityRule) {
        return new CityRuleSet(cityRule.getCityName(), cityRule.getRuleType(),
                cityRule.getCreditScore());
    }
}