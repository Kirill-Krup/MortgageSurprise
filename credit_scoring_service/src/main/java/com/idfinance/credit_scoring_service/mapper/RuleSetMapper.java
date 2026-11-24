package com.idfinance.credit_scoring_service.mapper;

import com.idfinance.credit_scoring_service.domain.rule_set.RuleSet;

import java.util.Set;

public interface RuleSetMapper<T extends RuleSet<E>, E, R> {
    Set<T> mapToRuleSet(Set<R> entities);
}