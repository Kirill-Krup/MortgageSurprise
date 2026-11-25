package com.idfinance.credit_scoring_service.facade.impl;

import com.idfinance.credit_scoring_service.domain.rule_set.CountryRuleSet;
import com.idfinance.credit_scoring_service.dto.CountryRuleRequest;
import com.idfinance.credit_scoring_service.facade.CountryRuleFacade;
import com.idfinance.credit_scoring_service.mapper.impl.CountryRuleSetMapper;
import com.idfinance.credit_scoring_service.service.CacheService;
import com.idfinance.credit_scoring_service.service.CountryRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class CountryRuleFacadeImpl implements CountryRuleFacade {

    private static final String NS = "countryRule";

    private final CountryRuleService countryRuleService;
    private final CountryRuleSetMapper countryRuleSetMapper;
    private final CacheService cacheService;

    @Override
    public Set<CountryRuleSet> findAll() {
        List<Integer> ids = countryRuleService.findAllIds();

        Map<Integer, CountryRuleSet> result = new HashMap<>(ids.size());
        List<Integer> missing = new ArrayList<>();

        for (Integer id : ids) {
            CountryRuleSet cached = cacheService.get(NS + ":" + id);
            if (cached != null) {
                result.put(id, cached);
            } else {
                missing.add(id);
            }
        }

        if (!missing.isEmpty()) {
            Set<CountryRuleSet> loaded =
                    countryRuleSetMapper.mapToRuleSet(countryRuleService.findAllById(missing));
            cacheService.putAll(NS, loaded);
            loaded.forEach(r -> result.put(r.getId(), r));
        }

        return new HashSet<>(result.values());
    }

    @Override
    public CountryRuleSet save(CountryRuleRequest countryRuleRequest) {
        return CountryRuleSet.from(countryRuleService.save(countryRuleRequest));
    }

    @Override
    public CountryRuleSet update(CountryRuleRequest countryRuleRequest, Integer id) {
        CountryRuleSet ruleSet = CountryRuleSet.from(countryRuleService.update(countryRuleRequest, id));
        cacheService.remove(NS + ":" + id);
        return ruleSet;
    }

    @Override
    public void deleteById(Integer id) {
        countryRuleService.deleteById(id);
        cacheService.remove(NS + ":" + id);
    }
}