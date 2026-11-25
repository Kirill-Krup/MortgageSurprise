package com.idfinance.credit_scoring_service.provider;

import com.idfinance.credit_scoring_service.domain.rule_set.RuleSet;

import java.util.Set;

public interface RuleProvider<T extends RuleSet<?>> {
    Set<T> getRules();
}
