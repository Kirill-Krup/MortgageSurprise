package com.idfinance.credit_scoring_service.service;

import com.idfinance.credit_scoring_service.dto.GenderRuleRequest;
import com.idfinance.credit_scoring_service.model.GenderRule;

import java.util.Set;

public interface GenderRuleService {
    Set<GenderRule> findAll();

    GenderRule save(GenderRuleRequest genderRuleRequest);

    GenderRule update(GenderRuleRequest genderRuleRequest, Integer id);

    void deleteById(Integer id);
}