package com.idfinance.credit_scoring_service.domain.rule_set;

import com.idfinance.credit_scoring_service.model.AgeRule;
import com.idfinance.credit_scoring_service.model.RuleType;

public record AgeRuleSet(int minAge, int maxAge, RuleType ruleType, float creditScore) implements RuleSet<Integer> {

    @Override
    public boolean apply(Integer integer) {
        switch (ruleType) {
            case INCLUSIVE -> {
                return minAge <= integer && integer <= maxAge;
            }
            case EXCLUSIVE -> {
                return minAge >= integer && integer >= maxAge;
            }
        }
        return false;
    }

    public static AgeRuleSet from(AgeRule ageRule) {
        return new AgeRuleSet(ageRule.getMinAge(), ageRule.getMaxAge(),
                ageRule.getRuleType(), ageRule.getCreditScore());
    }
}