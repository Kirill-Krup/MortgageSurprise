package com.idfinance.credit_scoring_service.adapter;

import com.idfinance.credit_scoring_service.domain.rule_set.GenderRuleSet;

import java.util.Set;

public interface GenderConfigAdapter {
    Set<GenderRuleSet> findAll();
}
