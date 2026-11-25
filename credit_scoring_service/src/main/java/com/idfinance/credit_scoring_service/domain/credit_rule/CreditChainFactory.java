package com.idfinance.credit_scoring_service.domain.credit_rule;


import com.idfinance.credit_scoring_service.domain.HandlerChainFactory;
import com.idfinance.credit_scoring_service.domain.rule_set.AgeRuleSet;
import com.idfinance.credit_scoring_service.domain.rule_set.CityRuleSet;
import com.idfinance.credit_scoring_service.domain.rule_set.CountryRuleSet;
import com.idfinance.credit_scoring_service.domain.rule_set.GenderRuleSet;
import com.idfinance.credit_scoring_service.provider.RuleProvider;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreditChainFactory implements HandlerChainFactory<CreditRuleChain> {

    private final RuleProvider<AgeRuleSet> ageRuleSetRuleProvider;
    private final RuleProvider<GenderRuleSet> genderRuleSetRuleProvider;
    private final RuleProvider<CountryRuleSet> countryRuleSetRuleProvider;
    private final RuleProvider<CityRuleSet> cityRuleSetRuleProvider;

    @Override
    public CreditRuleChain create() {
        return CreditChainBuilder.builder()
                .addBusinessRule(new AgeBusinessRule(ageRuleSetRuleProvider))
                .addBusinessRule(new GenderBusinessRule(genderRuleSetRuleProvider))
                .addBusinessRule(new CountryBusinessRule(countryRuleSetRuleProvider))
                .addBusinessRule(new CityBusinessRule(cityRuleSetRuleProvider))
                .build();
    }

    public CreditChainBuilder createCustom() {
        return CreditChainBuilder.builder();
    }
}