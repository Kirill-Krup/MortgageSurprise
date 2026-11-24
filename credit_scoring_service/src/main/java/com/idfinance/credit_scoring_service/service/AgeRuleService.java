package com.idfinance.credit_scoring_service.service;

import com.idfinance.credit_scoring_service.dto.AgeRuleRequest;
import com.idfinance.credit_scoring_service.model.AgeRule;

import java.util.Set;

public interface AgeRuleService {
    Set<AgeRule> findAll();

    AgeRule save(AgeRuleRequest ageRuleRequest);

    AgeRule update(AgeRuleRequest ageRuleRequest, Integer id);

    void deleteById(Integer id);
}