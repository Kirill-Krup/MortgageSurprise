package com.idfinance.credit_scoring_service.controller;

import com.idfinance.credit_scoring_service.domain.rule_set.CountryRuleSet;
import com.idfinance.credit_scoring_service.dto.CountryRuleRequest;
import com.idfinance.credit_scoring_service.facade.CountryRuleFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/rules/country-rules")
public class CountryRuleController {

    private final CountryRuleFacade countryRuleFacade;

    @GetMapping
    public ResponseEntity<Set<CountryRuleSet>> findAll() {
        return ResponseEntity.ok(countryRuleFacade.findAll());
    }

    @PostMapping
    public ResponseEntity<CountryRuleSet> save(@RequestBody CountryRuleRequest countryRuleRequest) {
        return ResponseEntity.ok(countryRuleFacade.save(countryRuleRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CountryRuleSet> update(@RequestBody CountryRuleRequest countryRuleRequest,
                                                 @PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(countryRuleFacade.update(countryRuleRequest, id));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        countryRuleFacade.deleteById(id);
    }
}