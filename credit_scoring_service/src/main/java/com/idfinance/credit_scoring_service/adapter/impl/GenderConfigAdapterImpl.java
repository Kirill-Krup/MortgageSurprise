package com.idfinance.credit_scoring_service.adapter.impl;

import com.idfinance.credit_scoring_service.adapter.GenderConfigAdapter;
import com.idfinance.credit_scoring_service.domain.rule_set.GenderRuleSet;
import com.idfinance.credit_scoring_service.mapper.impl.GenderRuleSetMapper;
import com.idfinance.credit_scoring_service.service.GenderRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class GenderConfigAdapterImpl implements GenderConfigAdapter {

    private final GenderRuleService genderRuleService;
    private final GenderRuleSetMapper genderRuleSetMapper;

    @Override
    public Set<GenderRuleSet> findAll() {
        return genderRuleSetMapper.mapToRuleSet(genderRuleService.findAll());
    }
}