package com.idfinance.credit_scoring_service.service;

import com.idfinance.credit_scoring_service.dto.CountryRuleRequest;
import com.idfinance.credit_scoring_service.model.CountryRule;

import java.util.Set;

public interface CountryRuleService {
    Set<CountryRule> findAll();

    CountryRule save(CountryRuleRequest genderRuleRequest);

    CountryRule update(CountryRuleRequest genderRuleRequest, Integer id);

    void deleteById(Integer id);
}