package com.idfinance.credit_scoring_service.service.impl;

import com.idfinance.credit_scoring_service.dto.CityRuleRequest;
import com.idfinance.credit_scoring_service.model.CityRule;
import com.idfinance.credit_scoring_service.repository.CityRuleRepository;
import com.idfinance.credit_scoring_service.service.CityRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public CityRule save(CityRuleRequest cityRuleRequest) {
        return cityRuleRepository.save(CityRuleRequest.asEntity(cityRuleRequest));
    }

    @Override
    @Transactional
    public CityRule update(CityRuleRequest cityRuleRequest, Long id) {
        var existing = cityRuleRepository.findById(id).orElseThrow(
                () -> new RuntimeException("City Rule Not Found"));
        existing.setCityName(cityRuleRequest.cityName());
        existing.setRuleType(cityRuleRequest.ruleType());
        existing.setCreditScore(cityRuleRequest.creditScore());
        return cityRuleRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        cityRuleRepository.deleteById(id);
    }
}