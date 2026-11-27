package com.surprise.mortgagesurprise.service;

import com.surprise.common.dto.scoring.*;
import com.surprise.mortgagesurprise.security.CustomUserDetails;

import java.util.Set;

public interface ScoringRulesAdminService {

    Set<AgeRuleDto> getAgeRules(CustomUserDetails userDetails);

    AgeRuleDto createAgeRule(CustomUserDetails userDetails, AgeRuleRequest request);

    AgeRuleDto updateAgeRule(CustomUserDetails userDetails, Integer id, AgeRuleRequest request);

    void deleteAgeRule(CustomUserDetails userDetails, Integer id);

    Set<GenderRuleDto> getGenderRules(CustomUserDetails userDetails);

    GenderRuleDto createGenderRule(CustomUserDetails userDetails, GenderRuleRequest request);

    GenderRuleDto updateGenderRule(CustomUserDetails userDetails, Integer id, GenderRuleRequest request);

    void deleteGenderRule(CustomUserDetails userDetails, Integer id);

    Set<CountryRuleDto> getCountryRules(CustomUserDetails userDetails);

    CountryRuleDto createCountryRule(CustomUserDetails userDetails, CountryRuleRequest request);

    CountryRuleDto updateCountryRule(CustomUserDetails userDetails, Integer id, CountryRuleRequest request);

    void deleteCountryRule(CustomUserDetails userDetails, Integer id);

    Set<CityRuleDto> getCityRules(CustomUserDetails userDetails);

    CityRuleDto createCityRule(CustomUserDetails userDetails, CityRuleRequest request);

    CityRuleDto updateCityRule(CustomUserDetails userDetails, Long id, CityRuleRequest request);

    void deleteCityRule(CustomUserDetails userDetails, Long id);
}
