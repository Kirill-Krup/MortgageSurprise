package com.idfinance.credit_scoring_service.service;

import com.idfinance.credit_scoring_service.dto.CityRuleRequest;
import com.idfinance.credit_scoring_service.model.CityRule;

import java.util.List;
import java.util.Set;

public interface CityRuleService {
    Set<CityRule> findAll();

    CityRule save(CityRuleRequest cityRuleRequest);

    CityRule update(CityRuleRequest cityRuleRequest, Long id);

    void deleteById(Long id);

    List<Long> findAllIds();

    Set<CityRule> findAllById(List<Long> missing);
}