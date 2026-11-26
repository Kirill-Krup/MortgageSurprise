package com.idfinance.credit_scoring_service.mapper.impl;

import com.idfinance.credit_scoring_service.domain.rule_set.GenderRuleSet;
import com.idfinance.credit_scoring_service.mapper.RuleSetMapper;
import com.idfinance.credit_scoring_service.model.GenderRule;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GenderRuleSetMapper implements RuleSetMapper<GenderRuleSet, String, GenderRule> {
    @Override
    public Set<GenderRuleSet> mapToRuleSet(Set<GenderRule> entities) {
        return entities.stream()
                .map(GenderRuleSet::from)
                .collect(Collectors.toSet());
    }
}