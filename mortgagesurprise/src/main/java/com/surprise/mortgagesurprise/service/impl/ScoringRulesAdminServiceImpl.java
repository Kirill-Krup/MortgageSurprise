package com.surprise.mortgagesurprise.service.impl;

import com.surprise.common.dto.scoring.*;
import com.surprise.mortgagesurprise.client.CreditScoringRulesClient;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.security.jwt.JwtTokenProvider;
import com.surprise.mortgagesurprise.service.ScoringRulesAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScoringRulesAdminServiceImpl implements ScoringRulesAdminService {

    private static final String DEFAULT_ADMIN_ROLE = "ROLE_ADMIN";

    private final CreditScoringRulesClient rulesClient;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Set<AgeRuleDto> getAgeRules(CustomUserDetails userDetails) {
        return rulesClient.getAgeRules(buildBearerToken(userDetails));
    }

    @Override
    public AgeRuleDto createAgeRule(CustomUserDetails userDetails, AgeRuleRequest request) {
        return rulesClient.createAgeRule(buildBearerToken(userDetails), request);
    }

    @Override
    public AgeRuleDto updateAgeRule(CustomUserDetails userDetails, Integer id, AgeRuleRequest request) {
        return rulesClient.updateAgeRule(buildBearerToken(userDetails), id, request);
    }

    @Override
    public void deleteAgeRule(CustomUserDetails userDetails, Integer id) {
        rulesClient.deleteAgeRule(buildBearerToken(userDetails), id);
    }

    @Override
    public Set<GenderRuleDto> getGenderRules(CustomUserDetails userDetails) {
        return rulesClient.getGenderRules(buildBearerToken(userDetails));
    }

    @Override
    public GenderRuleDto createGenderRule(CustomUserDetails userDetails, GenderRuleRequest request) {
        return rulesClient.createGenderRule(buildBearerToken(userDetails), request);
    }

    @Override
    public GenderRuleDto updateGenderRule(CustomUserDetails userDetails, Integer id, GenderRuleRequest request) {
        return rulesClient.updateGenderRule(buildBearerToken(userDetails), id, request);
    }

    @Override
    public void deleteGenderRule(CustomUserDetails userDetails, Integer id) {
        rulesClient.deleteGenderRule(buildBearerToken(userDetails), id);
    }

    @Override
    public Set<CountryRuleDto> getCountryRules(CustomUserDetails userDetails) {
        return rulesClient.getCountryRules(buildBearerToken(userDetails));
    }

    @Override
    public CountryRuleDto createCountryRule(CustomUserDetails userDetails, CountryRuleRequest request) {
        return rulesClient.createCountryRule(buildBearerToken(userDetails), request);
    }

    @Override
    public CountryRuleDto updateCountryRule(CustomUserDetails userDetails, Integer id, CountryRuleRequest request) {
        return rulesClient.updateCountryRule(buildBearerToken(userDetails), id, request);
    }

    @Override
    public void deleteCountryRule(CustomUserDetails userDetails, Integer id) {
        rulesClient.deleteCountryRule(buildBearerToken(userDetails), id);
    }

    @Override
    public Set<CityRuleDto> getCityRules(CustomUserDetails userDetails) {
        return rulesClient.getCityRules(buildBearerToken(userDetails));
    }

    @Override
    public CityRuleDto createCityRule(CustomUserDetails userDetails, CityRuleRequest request) {
        return rulesClient.createCityRule(buildBearerToken(userDetails), request);
    }

    @Override
    public CityRuleDto updateCityRule(CustomUserDetails userDetails, Long id, CityRuleRequest request) {
        return rulesClient.updateCityRule(buildBearerToken(userDetails), id, request);
    }

    @Override
    public void deleteCityRule(CustomUserDetails userDetails, Long id) {
        rulesClient.deleteCityRule(buildBearerToken(userDetails), id);
    }

    private String buildBearerToken(CustomUserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse(DEFAULT_ADMIN_ROLE);
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                userDetails.getId(), userDetails.getUsername(), role);
    }
}
