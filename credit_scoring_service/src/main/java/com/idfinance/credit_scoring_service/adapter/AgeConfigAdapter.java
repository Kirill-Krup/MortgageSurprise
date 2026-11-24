package com.idfinance.credit_scoring_service.adapter;

import com.idfinance.credit_scoring_service.domain.rule_set.AgeRuleSet;

import java.util.Set;

public interface AgeConfigAdapter {
    Set<AgeRuleSet> findAll();
}