package com.idfinance.credit_scoring_service.facade.impl;

import com.idfinance.credit_scoring_service.facade.AgeRuleFacade;
import com.idfinance.credit_scoring_service.domain.rule_set.AgeRuleSet;
import com.idfinance.credit_scoring_service.dto.AgeRuleRequest;
import com.idfinance.credit_scoring_service.mapper.impl.AgeRuleSetMapper;
import com.idfinance.credit_scoring_service.service.AgeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AgeRuleFacadeImpl implements AgeRuleFacade {

    private final AgeRuleService ageRuleService;
    private final AgeRuleSetMapper ageRuleSetMapper;

    @Override
    public Set<AgeRuleSet> findAll() {
        return ageRuleSetMapper.mapToRuleSet(ageRuleService.findAll());
    }

    @Override
    public AgeRuleSet save(AgeRuleRequest ageRuleRequest) {
        return AgeRuleSet.from(ageRuleService.save(ageRuleRequest));
    }

    @Override
    public AgeRuleSet update(AgeRuleRequest ageRuleRequest, Integer id) {
        return AgeRuleSet.from(ageRuleService.update(ageRuleRequest, id));
    }

    @Override
    public void deleteById(Integer id) {
        ageRuleService.deleteById(id);
    }
}