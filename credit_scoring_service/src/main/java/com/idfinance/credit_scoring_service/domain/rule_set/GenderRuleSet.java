package com.idfinance.credit_scoring_service.domain.rule_set;

import com.idfinance.credit_scoring_service.model.GenderRule;
import com.idfinance.credit_scoring_service.model.RuleType;

public record GenderRuleSet(Integer id, String gender, RuleType ruleType, Float creditScore) implements RuleSet<String>,
        Identifiable<Integer> {

    @Override
    public boolean apply(String s) {
        if (s == null || gender == null || ruleType == null) {
            return false;
        }
        return switch (ruleType) {
            case INCLUSIVE -> s.trim().equalsIgnoreCase(gender.trim());
            case EXCLUSIVE -> !s.trim().equalsIgnoreCase(gender.trim());
        };
    }

    public static GenderRuleSet from(GenderRule genderRule) {
        return new GenderRuleSet(genderRule.getId(), genderRule.getName(), genderRule.getRuleType(),
                genderRule.getCreditScore());
    }

    @Override
    public Integer getId() {
        return id;
    }
}