package com.idfinance.credit_scoring_service.domain.rule_set;

import com.idfinance.credit_scoring_service.model.AgeRule;
import com.idfinance.credit_scoring_service.model.RuleType;

public record AgeRuleSet(Integer id, Integer minAge, Integer maxAge, RuleType ruleType,
                         Float creditScore) implements RuleSet<Integer>, Identifiable<Integer> {

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
        return new AgeRuleSet(ageRule.getId(), ageRule.getMinAge(), ageRule.getMaxAge(),
                ageRule.getRuleType(), ageRule.getCreditScore());
    }

    @Override
    public Integer getId() {
        return id;
    }
}