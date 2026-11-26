package com.surprise.mortgagesurprise.client;

import com.surprise.common.dto.scoring.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Feign-клиент для управления правилами скоринга (/api/v1/rules/*) сервиса credit_scoring_service.
 */
@FeignClient(
        name = "credit-scoring-rules",
        url = "${credit.scoring.url}"
)
public interface CreditScoringRulesClient {

    @GetMapping("/rules/age-rules")
    Set<AgeRuleDto> getAgeRules(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);

    @PostMapping("/rules/age-rules")
    AgeRuleDto createAgeRule(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                             @RequestBody AgeRuleRequest request);

    @PutMapping("/rules/age-rules/{id}")
    AgeRuleDto updateAgeRule(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                             @PathVariable("id") Integer id,
                             @RequestBody AgeRuleRequest request);

    @DeleteMapping("/rules/age-rules/{id}")
    void deleteAgeRule(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                       @PathVariable("id") Integer id);

    @GetMapping("/rules/gender-rules")
    Set<GenderRuleDto> getGenderRules(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);

    @PostMapping("/rules/gender-rules")
    GenderRuleDto createGenderRule(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                   @RequestBody GenderRuleRequest request);

    @PutMapping("/rules/gender-rules/{id}")
    GenderRuleDto updateGenderRule(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                   @PathVariable("id") Integer id,
                                   @RequestBody GenderRuleRequest request);

    @DeleteMapping("/rules/gender-rules/{id}")
    void deleteGenderRule(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                          @PathVariable("id") Integer id);

    @GetMapping("/rules/country-rules")
    Set<CountryRuleDto> getCountryRules(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);

    @PostMapping("/rules/country-rules")
    CountryRuleDto createCountryRule(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                     @RequestBody CountryRuleRequest request);

    @PutMapping("/rules/country-rules/{id}")
    CountryRuleDto updateCountryRule(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                     @PathVariable("id") Integer id,
                                     @RequestBody CountryRuleRequest request);

    @DeleteMapping("/rules/country-rules/{id}")
    void deleteCountryRule(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                           @PathVariable("id") Integer id);

    @GetMapping("/rules/city-rules")
    Set<CityRuleDto> getCityRules(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);

    @PostMapping("/rules/city-rules")
    CityRuleDto createCityRule(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                               @RequestBody CityRuleRequest request);

    @PutMapping("/rules/city-rules/{id}")
    CityRuleDto updateCityRule(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                               @PathVariable("id") Long id,
                               @RequestBody CityRuleRequest request);

    @DeleteMapping("/rules/city-rules/{id}")
    void deleteCityRule(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                        @PathVariable("id") Long id);
}
