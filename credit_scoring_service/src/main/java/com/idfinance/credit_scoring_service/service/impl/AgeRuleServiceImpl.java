package com.idfinance.credit_scoring_service.service.impl;

import com.idfinance.credit_scoring_service.model.AgeRule;
import com.idfinance.credit_scoring_service.repository.AgeRuleRepository;
import com.idfinance.credit_scoring_service.service.AgeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AgeRuleServiceImpl implements AgeRuleService {

    private final AgeRuleRepository ageRuleRepository;

    @Override
    public Set<AgeRule> findAll() {
        return new HashSet<>(ageRuleRepository.findAll());
    }
}