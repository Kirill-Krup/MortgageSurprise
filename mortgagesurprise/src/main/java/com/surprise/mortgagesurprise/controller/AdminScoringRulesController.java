package com.surprise.mortgagesurprise.controller;

import com.surprise.common.dto.scoring.*;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.ScoringRulesAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/scoring-rules")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminScoringRulesController {

    private final ScoringRulesAdminService scoringRulesAdminService;

    @GetMapping("/age")
    public ResponseEntity<Set<AgeRuleDto>> getAgeRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(scoringRulesAdminService.getAgeRules(userDetails));
    }

    @PostMapping("/age")
    public ResponseEntity<AgeRuleDto> createAgeRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                    @Valid @RequestBody AgeRuleRequest request) {
        return ResponseEntity.ok(scoringRulesAdminService.createAgeRule(userDetails, request));
    }

    @PutMapping("/age/{id}")
    public ResponseEntity<AgeRuleDto> updateAgeRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                    @PathVariable Integer id,
                                                    @Valid @RequestBody AgeRuleRequest request) {
        return ResponseEntity.ok(scoringRulesAdminService.updateAgeRule(userDetails, id, request));
    }

    @DeleteMapping("/age/{id}")
    public ResponseEntity<Void> deleteAgeRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                              @PathVariable Integer id) {
        scoringRulesAdminService.deleteAgeRule(userDetails, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/gender")
    public ResponseEntity<Set<GenderRuleDto>> getGenderRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(scoringRulesAdminService.getGenderRules(userDetails));
    }

    @PostMapping("/gender")
    public ResponseEntity<GenderRuleDto> createGenderRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                          @Valid @RequestBody GenderRuleRequest request) {
        return ResponseEntity.ok(scoringRulesAdminService.createGenderRule(userDetails, request));
    }

    @PutMapping("/gender/{id}")
    public ResponseEntity<GenderRuleDto> updateGenderRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                          @PathVariable Integer id,
                                                          @Valid @RequestBody GenderRuleRequest request) {
        return ResponseEntity.ok(scoringRulesAdminService.updateGenderRule(userDetails, id, request));
    }

    @DeleteMapping("/gender/{id}")
    public ResponseEntity<Void> deleteGenderRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                 @PathVariable Integer id) {
        scoringRulesAdminService.deleteGenderRule(userDetails, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/country")
    public ResponseEntity<Set<CountryRuleDto>> getCountryRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(scoringRulesAdminService.getCountryRules(userDetails));
    }

    @PostMapping("/country")
    public ResponseEntity<CountryRuleDto> createCountryRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @Valid @RequestBody CountryRuleRequest request) {
        return ResponseEntity.ok(scoringRulesAdminService.createCountryRule(userDetails, request));
    }

    @PutMapping("/country/{id}")
    public ResponseEntity<CountryRuleDto> updateCountryRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @PathVariable Integer id,
                                                            @Valid @RequestBody CountryRuleRequest request) {
        return ResponseEntity.ok(scoringRulesAdminService.updateCountryRule(userDetails, id, request));
    }

    @DeleteMapping("/country/{id}")
    public ResponseEntity<Void> deleteCountryRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                  @PathVariable Integer id) {
        scoringRulesAdminService.deleteCountryRule(userDetails, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/city")
    public ResponseEntity<Set<CityRuleDto>> getCityRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(scoringRulesAdminService.getCityRules(userDetails));
    }

    @PostMapping("/city")
    public ResponseEntity<CityRuleDto> createCityRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                      @Valid @RequestBody CityRuleRequest request) {
        return ResponseEntity.ok(scoringRulesAdminService.createCityRule(userDetails, request));
    }

    @PutMapping("/city/{id}")
    public ResponseEntity<CityRuleDto> updateCityRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody CityRuleRequest request) {
        return ResponseEntity.ok(scoringRulesAdminService.updateCityRule(userDetails, id, request));
    }

    @DeleteMapping("/city/{id}")
    public ResponseEntity<Void> deleteCityRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                               @PathVariable Long id) {
        scoringRulesAdminService.deleteCityRule(userDetails, id);
        return ResponseEntity.noContent().build();
    }
}
