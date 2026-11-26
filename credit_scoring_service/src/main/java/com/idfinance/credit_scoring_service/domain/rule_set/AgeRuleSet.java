package com.idfinance.credit_scoring_service.domain.rule_set;

import com.idfinance.credit_scoring_service.model.AgeRule;
import com.idfinance.credit_scoring_service.model.RuleType;

public record AgeRuleSet(Integer id, Integer minAge, Integer maxAge, RuleType ruleType,
                         Float creditScore) implements RuleSet<Integer>, Identifiable<Integer> {

    @Override
    public boolean apply(Integer integer) {
        if (integer == null || minAge == null || maxAge == null || ruleType == null) {
            return false;
        }
        return switch (ruleType) {
            case INCLUSIVE -> minAge <= integer && integer <= maxAge;
            case EXCLUSIVE -> integer < minAge || integer > maxAge;
        };
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