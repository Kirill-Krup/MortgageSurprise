package com.idfinance.credit_scoring_service.service;

import com.idfinance.credit_scoring_service.model.CountryRule;

import java.util.Set;

public interface CountryRuleService {
    Set<CountryRule> findAll();
}