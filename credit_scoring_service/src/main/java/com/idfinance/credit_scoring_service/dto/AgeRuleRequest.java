package com.idfinance.credit_scoring_service.dto;

import com.idfinance.credit_scoring_service.model.AgeRule;
import com.idfinance.credit_scoring_service.model.RuleType;

public record AgeRuleRequest(int minAge, int maxAge, RuleType ruleType, Float creditScore) {
    public static AgeRule asEntity(AgeRuleRequest ageRuleRequest) {
        return new AgeRule(ageRuleRequest.minAge(), ageRuleRequest.maxAge(),
                ageRuleRequest.ruleType(), ageRuleRequest.creditScore());
    }
}