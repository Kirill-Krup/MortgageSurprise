package com.idfinance.credit_scoring_service.mapper.impl;

import com.idfinance.credit_scoring_service.domain.rule_set.AgeRuleSet;
import com.idfinance.credit_scoring_service.mapper.RuleSetMapper;
import com.idfinance.credit_scoring_service.model.AgeRule;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AgeRuleSetMapper implements RuleSetMapper<AgeRuleSet, Integer, AgeRule> {
    @Override
    public Set<AgeRuleSet> mapToRuleSet(Set<AgeRule> entities) {
        return entities.stream()
                .map(AgeRuleSet::from)
                .collect(Collectors.toSet());
    }
}