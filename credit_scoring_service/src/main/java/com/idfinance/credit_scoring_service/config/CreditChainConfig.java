package com.idfinance.credit_scoring_service.config;

import com.idfinance.credit_scoring_service.domain.credit_rule.CreditChainFactory;
import com.idfinance.credit_scoring_service.domain.credit_rule.CreditRuleChain;
import com.idfinance.credit_scoring_service.domain.rule_set.AgeRuleSet;
import com.idfinance.credit_scoring_service.domain.rule_set.CityRuleSet;
import com.idfinance.credit_scoring_service.domain.rule_set.CountryRuleSet;
import com.idfinance.credit_scoring_service.domain.rule_set.GenderRuleSet;
import com.idfinance.credit_scoring_service.provider.RuleProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CreditChainConfig {

    @Bean
    public CreditRuleChain creditRuleChain(RuleProvider<AgeRuleSet> ageRuleSetRuleProvider,
                                           RuleProvider<GenderRuleSet> genderRuleSetRuleProvider,
                                           RuleProvider<CountryRuleSet> countryRuleSetRuleProvider,
                                           RuleProvider<CityRuleSet> cityRuleSetRuleProvider) {
        return new CreditChainFactory(ageRuleSetRuleProvider, genderRuleSetRuleProvider,
                countryRuleSetRuleProvider, cityRuleSetRuleProvider)
                .create();
    }
}