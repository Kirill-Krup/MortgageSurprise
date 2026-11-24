package com.idfinance.credit_scoring_service.adapter.impl;

import com.idfinance.credit_scoring_service.adapter.AgeConfigAdapter;
import com.idfinance.credit_scoring_service.domain.rule_set.AgeRuleSet;
import com.idfinance.credit_scoring_service.mapper.impl.AgeRuleSetMapper;
import com.idfinance.credit_scoring_service.service.AgeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AgeConfigAdapterImpl implements AgeConfigAdapter {

    private final AgeRuleService ageRuleService;
    private final AgeRuleSetMapper ageRuleSetMapper;

    @Override
    public Set<AgeRuleSet> findAll() {
        return ageRuleSetMapper.mapToRuleSet(ageRuleService.findAll());
    }
}
