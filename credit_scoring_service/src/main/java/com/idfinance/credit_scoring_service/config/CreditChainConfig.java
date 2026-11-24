package com.idfinance.credit_scoring_service.config;

import com.idfinance.credit_scoring_service.adapter.AgeConfigAdapter;
import com.idfinance.credit_scoring_service.adapter.GenderConfigAdapter;
import com.idfinance.credit_scoring_service.adapter.LocationConfigAdapter;
import com.idfinance.credit_scoring_service.domain.credit_rule.CreditChainFactory;
import com.idfinance.credit_scoring_service.domain.credit_rule.CreditRuleChain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CreditChainConfig {

    @Bean
    public CreditRuleChain creditRuleChain(AgeConfigAdapter ageConfigAdapter,
                                           GenderConfigAdapter genderConfigAdapter,
                                           LocationConfigAdapter locationConfigAdapter) {
        return new CreditChainFactory(ageConfigAdapter, genderConfigAdapter, locationConfigAdapter)
                .create();
    }
}