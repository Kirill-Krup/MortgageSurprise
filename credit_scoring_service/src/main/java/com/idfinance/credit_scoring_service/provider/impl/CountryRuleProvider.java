package com.idfinance.credit_scoring_service.provider.impl;

import com.idfinance.credit_scoring_service.facade.CountryRuleFacade;
import com.idfinance.credit_scoring_service.domain.rule_set.CountryRuleSet;
import com.idfinance.credit_scoring_service.provider.RuleProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class CountryRuleProvider implements RuleProvider<CountryRuleSet> {

    private final CountryRuleFacade countryRuleFacade;

    @Override
    public Set<CountryRuleSet> getRules() {
        return countryRuleFacade.findAll();
    }
}
