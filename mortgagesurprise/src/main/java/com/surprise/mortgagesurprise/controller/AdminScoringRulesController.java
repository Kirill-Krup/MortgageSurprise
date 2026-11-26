package com.surprise.mortgagesurprise.controller;

import com.surprise.common.dto.scoring.*;
import com.surprise.mortgagesurprise.client.CreditScoringRulesClient;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/scoring-rules")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminScoringRulesController {

    private final CreditScoringRulesClient rulesClient;
    private final JwtTokenProvider jwtTokenProvider;

    private String bearerToken(CustomUserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_ADMIN");
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                userDetails.getId(), userDetails.getUsername(), role);
    }

    @GetMapping("/age")
    public ResponseEntity<Set<AgeRuleDto>> getAgeRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(rulesClient.getAgeRules(bearerToken(userDetails)));
    }

    @PostMapping("/age")
    public ResponseEntity<AgeRuleDto> createAgeRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                    @RequestBody AgeRuleRequest request) {
        return ResponseEntity.ok(rulesClient.createAgeRule(bearerToken(userDetails), request));
    }

    @PutMapping("/age/{id}")
    public ResponseEntity<AgeRuleDto> updateAgeRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                    @PathVariable Integer id,
                                                    @RequestBody AgeRuleRequest request) {
        return ResponseEntity.ok(rulesClient.updateAgeRule(bearerToken(userDetails), id, request));
    }

    @DeleteMapping("/age/{id}")
    public ResponseEntity<Void> deleteAgeRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                              @PathVariable Integer id) {
        rulesClient.deleteAgeRule(bearerToken(userDetails), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/gender")
    public ResponseEntity<Set<GenderRuleDto>> getGenderRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(rulesClient.getGenderRules(bearerToken(userDetails)));
    }

    @PostMapping("/gender")
    public ResponseEntity<GenderRuleDto> createGenderRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                          @RequestBody GenderRuleRequest request) {
        return ResponseEntity.ok(rulesClient.createGenderRule(bearerToken(userDetails), request));
    }

    @PutMapping("/gender/{id}")
    public ResponseEntity<GenderRuleDto> updateGenderRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                          @PathVariable Integer id,
                                                          @RequestBody GenderRuleRequest request) {
        return ResponseEntity.ok(rulesClient.updateGenderRule(bearerToken(userDetails), id, request));
    }

    @DeleteMapping("/gender/{id}")
    public ResponseEntity<Void> deleteGenderRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                 @PathVariable Integer id) {
        rulesClient.deleteGenderRule(bearerToken(userDetails), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/country")
    public ResponseEntity<Set<CountryRuleDto>> getCountryRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(rulesClient.getCountryRules(bearerToken(userDetails)));
    }

    @PostMapping("/country")
    public ResponseEntity<CountryRuleDto> createCountryRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @RequestBody CountryRuleRequest request) {
        return ResponseEntity.ok(rulesClient.createCountryRule(bearerToken(userDetails), request));
    }

    @PutMapping("/country/{id}")
    public ResponseEntity<CountryRuleDto> updateCountryRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @PathVariable Integer id,
                                                            @RequestBody CountryRuleRequest request) {
        return ResponseEntity.ok(rulesClient.updateCountryRule(bearerToken(userDetails), id, request));
    }

    @DeleteMapping("/country/{id}")
    public ResponseEntity<Void> deleteCountryRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                  @PathVariable Integer id) {
        rulesClient.deleteCountryRule(bearerToken(userDetails), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/city")
    public ResponseEntity<Set<CityRuleDto>> getCityRules(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(rulesClient.getCityRules(bearerToken(userDetails)));
    }

    @PostMapping("/city")
    public ResponseEntity<CityRuleDto> createCityRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                      @RequestBody CityRuleRequest request) {
        return ResponseEntity.ok(rulesClient.createCityRule(bearerToken(userDetails), request));
    }

    @PutMapping("/city/{id}")
    public ResponseEntity<CityRuleDto> updateCityRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                      @PathVariable Long id,
                                                      @RequestBody CityRuleRequest request) {
        return ResponseEntity.ok(rulesClient.updateCityRule(bearerToken(userDetails), id, request));
    }

    @DeleteMapping("/city/{id}")
    public ResponseEntity<Void> deleteCityRule(@AuthenticationPrincipal CustomUserDetails userDetails,
                                               @PathVariable Long id) {
        rulesClient.deleteCityRule(bearerToken(userDetails), id);
        return ResponseEntity.noContent().build();
    }
}
