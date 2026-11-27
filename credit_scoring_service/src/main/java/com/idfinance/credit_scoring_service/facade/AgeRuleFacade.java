package com.idfinance.credit_scoring_service.facade;

import com.idfinance.credit_scoring_service.domain.rule_set.AgeRuleSet;
import com.idfinance.credit_scoring_service.dto.AgeRuleRequest;

import java.util.Set;

public interface AgeRuleFacade {
    Set<AgeRuleSet> findAll();

    AgeRuleSet save(AgeRuleRequest ageRuleRequest);

    AgeRuleSet update(AgeRuleRequest ageRuleRequest, Integer id);

    void deleteById(Integer id);
}