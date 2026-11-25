package com.idfinance.credit_scoring_service.facade;

import com.idfinance.credit_scoring_service.domain.rule_set.CountryRuleSet;
import com.idfinance.credit_scoring_service.dto.CountryRuleRequest;

import java.util.Set;

public interface CountryRuleFacade {
    Set<CountryRuleSet> findAll();

    CountryRuleSet save(CountryRuleRequest countryRuleRequest);

    CountryRuleSet update(CountryRuleRequest countryRuleRequest, Integer id);

    void deleteById(Integer id);
}