package com.idfinance.credit_scoring_service.mapper.impl;

import com.idfinance.credit_scoring_service.domain.rule_set.CityRuleSet;
import com.idfinance.credit_scoring_service.mapper.RuleSetMapper;
import com.idfinance.credit_scoring_service.model.CityRule;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CityRuleSetMapper implements RuleSetMapper<CityRuleSet, String, CityRule> {
    @Override
    public Set<CityRuleSet> mapToRuleSet(Set<CityRule> entities) {
        return entities.stream()
                .map(CityRuleSet::from)
                .collect(Collectors.toSet());
    }
}