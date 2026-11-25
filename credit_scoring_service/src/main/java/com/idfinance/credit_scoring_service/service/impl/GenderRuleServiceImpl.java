package com.idfinance.credit_scoring_service.service.impl;

import com.idfinance.credit_scoring_service.dto.GenderRuleRequest;
import com.idfinance.credit_scoring_service.model.GenderRule;
import com.idfinance.credit_scoring_service.repository.GenderRuleRepository;
import com.idfinance.credit_scoring_service.service.GenderRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GenderRuleServiceImpl implements GenderRuleService {

    private final GenderRuleRepository genderRuleRepository;

    @Override
    public Set<GenderRule> findAll() {
        return new HashSet<>(genderRuleRepository.findAll());
    }

    @Override
    @Transactional
    public GenderRule save(GenderRuleRequest genderRuleRequest) {
        return genderRuleRepository.save(GenderRuleRequest.asEntity(genderRuleRequest));
    }

    @Override
    @Transactional
    public GenderRule update(GenderRuleRequest genderRuleRequest, Integer id) {
        var existing = genderRuleRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Gender Rule Not Found"));
        existing.setName(genderRuleRequest.name());
        existing.setRuleType(genderRuleRequest.ruleType());
        existing.setCreditScore(genderRuleRequest.creditScore());
        return genderRuleRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        genderRuleRepository.deleteById(id);
    }

    @Override
    public List<Integer> findAllIds() {
        return genderRuleRepository.findAllIds();
    }

    @Override
    public Set<GenderRule> findAllById(List<Integer> missing) {
        return new HashSet<>(genderRuleRepository.findAllById(missing));
    }
}