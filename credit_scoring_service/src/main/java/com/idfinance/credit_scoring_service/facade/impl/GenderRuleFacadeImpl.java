package com.idfinance.credit_scoring_service.facade.impl;

import com.idfinance.credit_scoring_service.facade.GenderRuleFacade;
import com.idfinance.credit_scoring_service.domain.rule_set.GenderRuleSet;
import com.idfinance.credit_scoring_service.dto.GenderRuleRequest;
import com.idfinance.credit_scoring_service.mapper.impl.GenderRuleSetMapper;
import com.idfinance.credit_scoring_service.service.GenderRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class GenderRuleFacadeImpl implements GenderRuleFacade {

    private final GenderRuleService genderRuleService;
    private final GenderRuleSetMapper genderRuleSetMapper;

    @Override
    public Set<GenderRuleSet> findAll() {
        return genderRuleSetMapper.mapToRuleSet(genderRuleService.findAll());
    }

    @Override
    public GenderRuleSet save(GenderRuleRequest genderRuleRequest) {
        return GenderRuleSet.from(genderRuleService.save(genderRuleRequest));
    }

    @Override
    public GenderRuleSet update(GenderRuleRequest genderRuleRequest, Integer id) {
        return GenderRuleSet.from(genderRuleService.update(genderRuleRequest, id));
    }

    @Override
    public void deleteById(Integer id) {
        genderRuleService.deleteById(id);
    }
}