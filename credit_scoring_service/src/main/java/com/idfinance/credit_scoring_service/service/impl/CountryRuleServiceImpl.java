package com.idfinance.credit_scoring_service.service.impl;

import com.idfinance.credit_scoring_service.dto.CountryRuleRequest;
import com.idfinance.credit_scoring_service.model.CountryRule;
import com.idfinance.credit_scoring_service.repository.CountryRuleRepository;
import com.idfinance.credit_scoring_service.service.CountryRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CountryRuleServiceImpl implements CountryRuleService {

    private final CountryRuleRepository countryRuleRepository;

    @Override
    public Set<CountryRule> findAll() {
        return new HashSet<>(countryRuleRepository.findAll());
    }

    @Override
    @Transactional
    public CountryRule save(CountryRuleRequest countryRuleRequest) {
        return countryRuleRepository.save(CountryRuleRequest.asEntity(countryRuleRequest));
    }

    @Override
    @Transactional
    public CountryRule update(CountryRuleRequest countryRuleRequest, Integer id) {
        var existing = countryRuleRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Country Rule Not Found"));
        existing.setCountryCode(countryRuleRequest.countryCode());
        existing.setRuleType(countryRuleRequest.ruleType());
        existing.setCreditScore(countryRuleRequest.creditScore());
        return countryRuleRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        countryRuleRepository.deleteById(id);
    }

    @Override
    public Set<CountryRule> findAllById(List<Integer> missing) {
        return new HashSet<>(countryRuleRepository.findAllById(missing));
    }

    @Override
    public List<Integer> findAllIds() {
        return countryRuleRepository.findAllIds();
    }
}