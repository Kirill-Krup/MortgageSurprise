package com.idfinance.credit_scoring_service.facade.impl;

import com.idfinance.credit_scoring_service.domain.rule_set.CityRuleSet;
import com.idfinance.credit_scoring_service.dto.CityRuleRequest;
import com.idfinance.credit_scoring_service.facade.CityRuleFacade;
import com.idfinance.credit_scoring_service.mapper.impl.CityRuleSetMapper;
import com.idfinance.credit_scoring_service.service.CacheService;
import com.idfinance.credit_scoring_service.service.CityRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class CityRuleFacadeImpl implements CityRuleFacade {

    private static final String NS = "cityRule";

    private final CityRuleService cityRuleService;
    private final CityRuleSetMapper cityRuleSetMapper;
    private final CacheService cacheService;

    @Override
    public Set<CityRuleSet> findAll() {
        List<Long> ids = cityRuleService.findAllIds();

        Map<Long, CityRuleSet> result = new HashMap<>(ids.size());
        List<Long> missing = new ArrayList<>();

        for (Long id : ids) {
            CityRuleSet cached = cacheService.get(NS + ":" + id);
            if (cached != null) {
                result.put(id, cached);
            } else {
                missing.add(id);
            }
        }

        if (!missing.isEmpty()) {
            Set<CityRuleSet> loaded =
                    cityRuleSetMapper.mapToRuleSet(cityRuleService.findAllById(missing));
            cacheService.putAll(NS, loaded);
            loaded.forEach(r -> result.put(r.getId(), r));
        }

        return new HashSet<>(result.values());
    }

    @Override
    public CityRuleSet save(CityRuleRequest cityRuleRequest) {
        return CityRuleSet.from(cityRuleService.save(cityRuleRequest));
    }

    @Override
    public CityRuleSet update(CityRuleRequest cityRuleRequest, Long id) {
        CityRuleSet ruleSet = CityRuleSet.from(cityRuleService.update(cityRuleRequest, id));
        cacheService.remove(NS + ":" + id);
        return ruleSet;
    }

    @Override
    public void deleteById(Long id) {
        cityRuleService.deleteById(id);
        cacheService.remove(NS + ":" + id);
    }
}