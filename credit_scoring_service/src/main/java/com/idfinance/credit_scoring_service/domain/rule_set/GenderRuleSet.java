package com.idfinance.credit_scoring_service.domain.rule_set;

import com.idfinance.credit_scoring_service.model.GenderRule;
import com.idfinance.credit_scoring_service.model.RuleType;

public record GenderRuleSet(String gender, RuleType ruleType, Float creditScore) implements RuleSet<String> {

    @Override
    public boolean apply(String s) {
        switch (ruleType) {
            case INCLUSIVE -> {
                return s.trim().equals(gender);
            }
            case EXCLUSIVE -> {
                return !(s.trim().equals(gender));
            }
        }
        return false;
    }

    public static GenderRuleSet from(GenderRule genderRule) {
        return new GenderRuleSet(genderRule.getName(), genderRule.getRuleType(),
                genderRule.getCreditScore());
    }
}