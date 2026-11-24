package com.idfinance.credit_scoring_service.adapter.impl;

import com.idfinance.credit_scoring_service.adapter.LocationConfigAdapter;
import com.idfinance.credit_scoring_service.domain.rule_set.CityRuleSet;
import com.idfinance.credit_scoring_service.domain.rule_set.CountryRuleSet;
import com.idfinance.credit_scoring_service.mapper.impl.CityRuleSetMapper;
import com.idfinance.credit_scoring_service.mapper.impl.CountryRuleSetMapper;
import com.idfinance.credit_scoring_service.service.CityRuleService;
import com.idfinance.credit_scoring_service.service.CountryRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class LocationConfigAdapterImpl implements LocationConfigAdapter {

    private final CountryRuleService countryRuleService;
    private final CountryRuleSetMapper countryRuleSetMapper;
    private final CityRuleService cityRuleService;
    private final CityRuleSetMapper cityRuleSetMapper;

    @Override
    public Set<CountryRuleSet> findAllCountry() {
        return countryRuleSetMapper.mapToRuleSet(countryRuleService.findAll());
    }

    @Override
    public Set<CityRuleSet> findAllCity() {
        return cityRuleSetMapper.mapToRuleSet(cityRuleService.findAll());
    }
}