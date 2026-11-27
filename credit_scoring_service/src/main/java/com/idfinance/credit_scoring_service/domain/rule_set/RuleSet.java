package com.idfinance.credit_scoring_service.domain.rule_set;

public sealed interface RuleSet<T> permits AgeRuleSet, CityRuleSet,
        CountryRuleSet, GenderRuleSet {
    boolean apply(T t);
}