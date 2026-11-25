package com.idfinance.credit_scoring_service.controller;

import com.idfinance.credit_scoring_service.domain.rule_set.CityRuleSet;
import com.idfinance.credit_scoring_service.dto.CityRuleRequest;
import com.idfinance.credit_scoring_service.facade.CityRuleFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/rules/city-rules")
public class CityRuleController {

    private final CityRuleFacade cityRuleFacade;

    @GetMapping
    public ResponseEntity<Set<CityRuleSet>> findAll() {
        return ResponseEntity.ok(cityRuleFacade.findAll());
    }

    @PostMapping
    public ResponseEntity<CityRuleSet> save(@RequestBody CityRuleRequest cityRuleRequest) {
        return ResponseEntity.ok(cityRuleFacade.save(cityRuleRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CityRuleSet> update(@RequestBody CityRuleRequest cityRuleRequest,
                                              @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(cityRuleFacade.update(cityRuleRequest, id));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        cityRuleFacade.deleteById(id);
    }
}