package com.idfinance.credit_scoring_service.facade.impl;

import com.idfinance.credit_scoring_service.facade.GenderRuleFacade;
import com.idfinance.credit_scoring_service.domain.rule_set.GenderRuleSet;
import com.idfinance.credit_scoring_service.dto.GenderRuleRequest;
import com.idfinance.credit_scoring_service.mapper.impl.GenderRuleSetMapper;
import com.idfinance.credit_scoring_service.service.CacheService;
import com.idfinance.credit_scoring_service.service.GenderRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class GenderRuleFacadeImpl implements GenderRuleFacade {

    private static final String NS = "genderRule";

    private final GenderRuleService genderRuleService;
    private final GenderRuleSetMapper genderRuleSetMapper;
    private final CacheService cacheService;

    @Override
    public Set<GenderRuleSet> findAll() {
        List<Integer> ids = genderRuleService.findAllIds();

        Map<Integer, GenderRuleSet> result = new HashMap<>(ids.size());
        List<Integer> missing = new ArrayList<>();

        for (Integer id : ids) {
            GenderRuleSet cached = cacheService.get(NS + ":" + id);
            if (cached != null) {
                result.put(id, cached);
            } else {
                missing.add(id);
            }
        }

        if (!missing.isEmpty()) {
            Set<GenderRuleSet> loaded =
                    genderRuleSetMapper.mapToRuleSet(genderRuleService.findAllById(missing));
            cacheService.putAll(NS, loaded);
            loaded.forEach(r -> result.put(r.getId(), r));
        }

        return new HashSet<>(result.values());
    }

    @Override
    public GenderRuleSet save(GenderRuleRequest genderRuleRequest) {
        return GenderRuleSet.from(genderRuleService.save(genderRuleRequest));
    }

    @Override
    public GenderRuleSet update(GenderRuleRequest genderRuleRequest, Integer id) {
        GenderRuleSet ruleSet = GenderRuleSet.from(genderRuleService.update(genderRuleRequest, id));
        cacheService.remove(NS + ":" + id);
        return ruleSet;
    }

    @Override
    public void deleteById(Integer id) {
        genderRuleService.deleteById(id);
        cacheService.remove(NS + ":" + id);
    }
}