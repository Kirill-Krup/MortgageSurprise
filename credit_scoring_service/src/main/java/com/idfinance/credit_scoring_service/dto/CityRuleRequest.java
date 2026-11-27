package com.idfinance.credit_scoring_service.dto;

import com.idfinance.credit_scoring_service.model.CityRule;
import com.idfinance.credit_scoring_service.model.RuleType;

public record CityRuleRequest(String cityName, RuleType ruleType, Float creditScore) {
    public static CityRule asEntity(CityRuleRequest cityRuleRequest) {
        return new CityRule(cityRuleRequest.cityName(), cityRuleRequest.ruleType(),
                cityRuleRequest.creditScore());
    }
}