package com.idfinance.credit_scoring_service.adapter;

import com.idfinance.credit_scoring_service.domain.rule_set.CityRuleSet;
import com.idfinance.credit_scoring_service.domain.rule_set.CountryRuleSet;

import java.util.Set;

public interface LocationConfigAdapter {
    Set<CountryRuleSet> findAllCountry();

    Set<CityRuleSet> findAllCity();
}
