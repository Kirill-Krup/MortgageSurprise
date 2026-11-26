package com.idfinance.credit_scoring_service.dto;

import com.idfinance.credit_scoring_service.model.GenderRule;
import com.idfinance.credit_scoring_service.model.RuleType;

public record GenderRuleRequest(String name, RuleType ruleType, Float creditScore) {
    public static GenderRule asEntity(GenderRuleRequest genderRuleRequest) {
        return new GenderRule(genderRuleRequest.name, genderRuleRequest.ruleType,
                genderRuleRequest.creditScore);
    }
}