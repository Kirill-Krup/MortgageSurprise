package com.idfinance.credit_scoring_service.service.impl;

import com.idfinance.credit_scoring_service.model.CountryRule;
import com.idfinance.credit_scoring_service.repository.CountryRuleRepository;
import com.idfinance.credit_scoring_service.service.CountryRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CountryRuleServiceImpl implements CountryRuleService {
    private final CountryRuleRepository countryRuleRepository;

    @Override
    public Set<CountryRule> findAll() {
        return new HashSet<>(countryRuleRepository.findAll());
    }
}