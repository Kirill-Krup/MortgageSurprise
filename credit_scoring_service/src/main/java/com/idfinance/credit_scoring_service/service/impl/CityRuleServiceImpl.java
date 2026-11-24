package com.idfinance.credit_scoring_service.service.impl;

import com.idfinance.credit_scoring_service.model.CityRule;
import com.idfinance.credit_scoring_service.repository.CityRuleRepository;
import com.idfinance.credit_scoring_service.service.CityRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CityRuleServiceImpl implements CityRuleService {

    private final CityRuleRepository cityRuleRepository;

    @Override
    public Set<CityRule> findAll() {
        return new HashSet<>(cityRuleRepository.findAll());
    }
}