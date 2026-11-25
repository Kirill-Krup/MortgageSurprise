package com.idfinance.credit_scoring_service.service;

import com.idfinance.credit_scoring_service.dto.CountryRuleRequest;
import com.idfinance.credit_scoring_service.model.CountryRule;

import java.util.List;
import java.util.Set;

public interface CountryRuleService {
    Set<CountryRule> findAll();

    CountryRule save(CountryRuleRequest countryRuleRequest);

    CountryRule update(CountryRuleRequest countryRuleRequest, Integer id);

    void deleteById(Integer id);

    Set<CountryRule> findAllById(List<Integer> missing);

    List<Integer> findAllIds();
}