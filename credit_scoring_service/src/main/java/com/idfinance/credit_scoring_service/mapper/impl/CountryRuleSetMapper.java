package com.idfinance.credit_scoring_service.mapper.impl;

import com.idfinance.credit_scoring_service.domain.rule_set.CountryRuleSet;
import com.idfinance.credit_scoring_service.mapper.RuleSetMapper;
import com.idfinance.credit_scoring_service.model.CountryRule;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CountryRuleSetMapper implements RuleSetMapper<CountryRuleSet, String, CountryRule> {
    @Override
    public Set<CountryRuleSet> mapToRuleSet(Set<CountryRule> entities) {
        return entities.stream()
                .map(CountryRuleSet::from)
                .collect(Collectors.toSet());
    }
}