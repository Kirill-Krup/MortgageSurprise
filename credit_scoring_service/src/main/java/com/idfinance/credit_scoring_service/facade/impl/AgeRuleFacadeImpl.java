package com.idfinance.credit_scoring_service.facade.impl;

import com.idfinance.credit_scoring_service.domain.rule_set.AgeRuleSet;
import com.idfinance.credit_scoring_service.dto.AgeRuleRequest;
import com.idfinance.credit_scoring_service.facade.AgeRuleFacade;
import com.idfinance.credit_scoring_service.mapper.impl.AgeRuleSetMapper;
import com.idfinance.credit_scoring_service.service.AgeRuleService;
import com.idfinance.credit_scoring_service.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class AgeRuleFacadeImpl implements AgeRuleFacade {

    private static final String NS = "ageRule";

    private final AgeRuleService ageRuleService;
    private final AgeRuleSetMapper ageRuleSetMapper;
    private final CacheService cacheService;

    @Override
    public Set<AgeRuleSet> findAll() {
        List<Integer> ids = ageRuleService.findAllIds();
        Map<Integer, AgeRuleSet> result = new HashMap<>(ids.size());
        List<Integer> missing = new ArrayList<>();
        for (var id : ids) {
            AgeRuleSet cached = cacheService.get(NS + ":" + id);
            if (cached != null) {
                result.put(id, cached);
            } else {
                missing.add(id);
            }
        }
        if (!missing.isEmpty()) {
            Set<AgeRuleSet> loaded =
                    ageRuleSetMapper.mapToRuleSet(ageRuleService.findAllById(missing));
            cacheService.putAll(NS, loaded);
            loaded.forEach(r -> result.put(r.getId(), r));
        }
        return new HashSet<>(result.values());
    }

    @Override
    public AgeRuleSet save(AgeRuleRequest ageRuleRequest) {
        return AgeRuleSet.from(ageRuleService.save(ageRuleRequest));
    }

    @Override
    public AgeRuleSet update(AgeRuleRequest ageRuleRequest, Integer id) {
        AgeRuleSet ruleSet = AgeRuleSet.from(ageRuleService.update(ageRuleRequest, id));
        cacheService.remove(NS + ":" + id);
        return ruleSet;
    }

    @Override
    public void deleteById(Integer id) {
        ageRuleService.deleteById(id);
        cacheService.remove(NS + ":" + id);
    }
}