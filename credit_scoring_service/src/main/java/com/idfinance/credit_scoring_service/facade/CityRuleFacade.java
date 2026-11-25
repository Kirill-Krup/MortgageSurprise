package com.idfinance.credit_scoring_service.facade;

import com.idfinance.credit_scoring_service.domain.rule_set.CityRuleSet;
import com.idfinance.credit_scoring_service.dto.CityRuleRequest;

import java.util.Set;

public interface CityRuleFacade {
    Set<CityRuleSet> findAll();

    CityRuleSet save(CityRuleRequest cityRuleRequest);

    CityRuleSet update(CityRuleRequest cityRuleRequest, Long id);

    void deleteById(Long id);
}
