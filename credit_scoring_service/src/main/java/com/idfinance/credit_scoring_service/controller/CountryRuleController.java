package com.idfinance.credit_scoring_service.controller;

import com.idfinance.credit_scoring_service.domain.rule_set.CountryRuleSet;
import com.idfinance.credit_scoring_service.dto.CountryRuleRequest;
import com.idfinance.credit_scoring_service.service.CountryRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/rules/country-rules")
public class CountryRuleController {

    private final CountryRuleService countryRuleService;

    @GetMapping
    public ResponseEntity<List<CountryRuleSet>> findAll() {
        return ResponseEntity.ok(countryRuleService.findAll()
                .stream()
                .map(CountryRuleSet::from)
                .collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<CountryRuleSet> save(@RequestBody CountryRuleRequest countryRuleRequest) {
        return ResponseEntity.ok(CountryRuleSet.from(countryRuleService.save(countryRuleRequest)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CountryRuleSet> update(@RequestBody CountryRuleRequest countryRuleRequest,
                                                 @PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(CountryRuleSet.from(countryRuleService.update(countryRuleRequest, id)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        countryRuleService.deleteById(id);
    }
}