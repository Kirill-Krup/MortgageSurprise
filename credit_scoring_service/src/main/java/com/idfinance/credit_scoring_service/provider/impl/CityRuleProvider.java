package com.idfinance.credit_scoring_service.provider.impl;

import com.idfinance.credit_scoring_service.domain.rule_set.CityRuleSet;
import com.idfinance.credit_scoring_service.provider.RuleProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class CityRuleProvider implements RuleProvider<CityRuleSet> {
    @Override
    public Set<CityRuleSet> getRules() {
        return Set.of();
    }
}
