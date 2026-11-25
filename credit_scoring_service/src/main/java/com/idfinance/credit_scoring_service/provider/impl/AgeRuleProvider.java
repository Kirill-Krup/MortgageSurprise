package com.idfinance.credit_scoring_service.provider.impl;

import com.idfinance.credit_scoring_service.facade.AgeRuleFacade;
import com.idfinance.credit_scoring_service.domain.rule_set.AgeRuleSet;
import com.idfinance.credit_scoring_service.provider.RuleProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AgeRuleProvider implements RuleProvider<AgeRuleSet> {

    private final AgeRuleFacade ageRuleFacade;

    @Override
    public Set<AgeRuleSet> getRules() {
        return ageRuleFacade.findAll();
    }
}