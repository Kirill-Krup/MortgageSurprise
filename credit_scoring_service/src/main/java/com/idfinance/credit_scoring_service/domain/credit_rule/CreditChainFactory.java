package com.idfinance.credit_scoring_service.domain.credit_rule;


import com.idfinance.credit_scoring_service.adapter.AgeConfigAdapter;
import com.idfinance.credit_scoring_service.adapter.GenderConfigAdapter;
import com.idfinance.credit_scoring_service.adapter.LocationConfigAdapter;
import com.idfinance.credit_scoring_service.domain.HandlerChainFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreditChainFactory implements HandlerChainFactory<CreditRuleChain> {

    private final AgeConfigAdapter ageConfigAdapter;
    private final GenderConfigAdapter genderConfigAdapter;
    private final LocationConfigAdapter locationConfigAdapter;

    @Override
    public CreditRuleChain create() {
        return CreditChainBuilder.builder()
                .addBusinessRule(new AgeBusinessRule(ageConfigAdapter.findAll()))
                .addBusinessRule(new GenderBusinessRule(genderConfigAdapter.findAll()))
                .addBusinessRule(new LocationBusinessRule(locationConfigAdapter.findAllCity(),
                        locationConfigAdapter.findAllCountry()))
                .build();
    }

    public CreditChainBuilder createCustom() {
        return CreditChainBuilder.builder();
    }
}