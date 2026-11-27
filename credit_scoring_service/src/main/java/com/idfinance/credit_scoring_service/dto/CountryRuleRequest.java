package com.idfinance.credit_scoring_service.dto;

import com.idfinance.credit_scoring_service.model.CountryRule;
import com.idfinance.credit_scoring_service.model.RuleType;

public record CountryRuleRequest(String countryCode, RuleType ruleType, Float creditScore) {
    public static CountryRule asEntity(CountryRuleRequest countryRuleRequest) {
        return new CountryRule(countryRuleRequest.countryCode, countryRuleRequest.ruleType,
                countryRuleRequest.creditScore);
    }
}
