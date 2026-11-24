package com.idfinance.credit_scoring_service.service.impl;

import com.idfinance.credit_scoring_service.model.GenderRule;
import com.idfinance.credit_scoring_service.repository.GenderRuleRepository;
import com.idfinance.credit_scoring_service.service.GenderRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GenderRuleServiceImpl implements GenderRuleService {

    private final GenderRuleRepository genderRuleRepository;

    @Override
    public Set<GenderRule> findAll() {
        return new HashSet<>(genderRuleRepository.findAll());
    }
}