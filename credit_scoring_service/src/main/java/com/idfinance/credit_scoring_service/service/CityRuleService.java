package com.idfinance.credit_scoring_service.service;

import com.idfinance.credit_scoring_service.model.CityRule;

import java.util.Set;

public interface CityRuleService {
    Set<CityRule> findAll();
}