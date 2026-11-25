package com.idfinance.credit_scoring_service.facade.impl;

import com.idfinance.credit_scoring_service.domain.rule_set.CityRuleSet;
import com.idfinance.credit_scoring_service.dto.CityRuleRequest;
import com.idfinance.credit_scoring_service.facade.CityRuleFacade;
import com.idfinance.credit_scoring_service.mapper.impl.CityRuleSetMapper;
import com.idfinance.credit_scoring_service.service.CityRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class CityRuleFacadeImpl implements CityRuleFacade {

    private final CityRuleService cityRuleService;
    private final CityRuleSetMapper cityRuleSetMapper;

    @Override
    public Set<CityRuleSet> findAll() {
        return cityRuleSetMapper.mapToRuleSet(cityRuleService.findAll());
    }

    @Override
    public CityRuleSet save(CityRuleRequest genderRuleRequest) {
        return CityRuleSet.from(cityRuleService.save(genderRuleRequest));
    }

    @Override
    public CityRuleSet update(CityRuleRequest genderRuleRequest, Long id) {
        return CityRuleSet.from(cityRuleService.update(genderRuleRequest, id));
    }

    @Override
    public void deleteById(Long id) {
        cityRuleService.deleteById(id);
    }
}