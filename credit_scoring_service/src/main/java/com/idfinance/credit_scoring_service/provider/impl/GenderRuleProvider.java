package com.idfinance.credit_scoring_service.provider.impl;

import com.idfinance.credit_scoring_service.facade.GenderRuleFacade;
import com.idfinance.credit_scoring_service.domain.rule_set.GenderRuleSet;
import com.idfinance.credit_scoring_service.provider.RuleProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class GenderRuleProvider implements RuleProvider<GenderRuleSet> {

    private final GenderRuleFacade genderRuleFacade;

    @Override
    public Set<GenderRuleSet> getRules() {
        return genderRuleFacade.findAll();
    }
}