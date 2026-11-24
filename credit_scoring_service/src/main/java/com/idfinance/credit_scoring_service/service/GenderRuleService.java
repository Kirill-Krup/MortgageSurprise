package com.idfinance.credit_scoring_service.service;

import com.idfinance.credit_scoring_service.model.GenderRule;

import java.util.Set;

public interface GenderRuleService {
    Set<GenderRule> findAll();
}