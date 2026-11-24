package com.idfinance.credit_scoring_service.service.impl;

import com.idfinance.credit_scoring_service.dto.AgeRuleRequest;
import com.idfinance.credit_scoring_service.model.AgeRule;
import com.idfinance.credit_scoring_service.repository.AgeRuleRepository;
import com.idfinance.credit_scoring_service.service.AgeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public AgeRule save(AgeRuleRequest ageRuleRequest) {
        return ageRuleRepository.save(AgeRuleRequest.asEntity(ageRuleRequest));
    }

    @Override
    @Transactional
    public AgeRule update(AgeRuleRequest ageRuleRequest, Integer id) {
        var existing = ageRuleRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Age Rule Not Found"));
        existing.setMinAge(ageRuleRequest.minAge());
        existing.setMaxAge(ageRuleRequest.maxAge());
        existing.setRuleType(ageRuleRequest.ruleType());
        existing.setCreditScore(ageRuleRequest.creditScore());
        return ageRuleRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        ageRuleRepository.deleteById(id);
    }
}