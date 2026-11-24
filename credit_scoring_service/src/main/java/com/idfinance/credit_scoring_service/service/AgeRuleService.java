package com.idfinance.credit_scoring_service.service;

import com.idfinance.credit_scoring_service.model.AgeRule;

import java.util.Set;

public interface AgeRuleService {
    Set<AgeRule> findAll();
}