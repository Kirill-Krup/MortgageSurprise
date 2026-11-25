package com.idfinance.credit_scoring_service.facade.impl;

import com.idfinance.credit_scoring_service.domain.rule_set.CountryRuleSet;
import com.idfinance.credit_scoring_service.dto.CountryRuleRequest;
import com.idfinance.credit_scoring_service.facade.CountryRuleFacade;
import com.idfinance.credit_scoring_service.mapper.impl.CountryRuleSetMapper;
import com.idfinance.credit_scoring_service.service.CountryRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class CountryRuleFacadeImpl implements CountryRuleFacade {

    private final CountryRuleService countryRuleService;
    private final CountryRuleSetMapper countryRuleSetMapper;

    @Override
    public Set<CountryRuleSet> findAll() {
        return countryRuleSetMapper.mapToRuleSet(countryRuleService.findAll());
    }

    @Override
    public CountryRuleSet save(CountryRuleRequest genderRuleRequest) {
        return CountryRuleSet.from(countryRuleService.save(genderRuleRequest));
    }

    @Override
    public CountryRuleSet update(CountryRuleRequest genderRuleRequest, Integer id) {
        return CountryRuleSet.from(countryRuleService.update(genderRuleRequest, id));
    }

    @Override
    public void deleteById(Integer id) {
        countryRuleService.deleteById(id);
    }
}