package com.idfinance.credit_scoring_service.facade;

import com.idfinance.credit_scoring_service.domain.rule_set.GenderRuleSet;
import com.idfinance.credit_scoring_service.dto.GenderRuleRequest;

import java.util.Set;

public interface GenderRuleFacade {
    Set<GenderRuleSet> findAll();

    GenderRuleSet save(GenderRuleRequest genderRuleRequest);

    GenderRuleSet update(GenderRuleRequest genderRuleRequest, Integer id);

    void deleteById(Integer id);
}