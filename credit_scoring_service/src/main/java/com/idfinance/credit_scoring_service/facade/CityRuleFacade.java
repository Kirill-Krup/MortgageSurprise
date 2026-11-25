package com.idfinance.credit_scoring_service.facade;

import com.idfinance.credit_scoring_service.domain.rule_set.CityRuleSet;
import com.idfinance.credit_scoring_service.dto.CityRuleRequest;
import com.idfinance.credit_scoring_service.model.CityRule;

import java.util.Set;

public interface CityRuleFacade {
    Set<CityRuleSet> findAll();

    CityRuleSet save(CityRuleRequest genderRuleRequest);

    CityRuleSet update(CityRuleRequest genderRuleRequest, Long id);

    void deleteById(Long id);
}
