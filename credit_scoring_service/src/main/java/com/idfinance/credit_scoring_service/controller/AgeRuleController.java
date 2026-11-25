package com.idfinance.credit_scoring_service.controller;

import com.idfinance.credit_scoring_service.facade.AgeRuleFacade;
import com.idfinance.credit_scoring_service.domain.rule_set.AgeRuleSet;
import com.idfinance.credit_scoring_service.dto.AgeRuleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/rules/age-rules")
public class AgeRuleController {

    private final AgeRuleFacade ageRuleFacade;

    @GetMapping
    public ResponseEntity<Set<AgeRuleSet>> findAll() {
        return ResponseEntity.ok(ageRuleFacade.findAll());
    }

    @PostMapping
    public ResponseEntity<AgeRuleSet> save(@RequestBody AgeRuleRequest ageRuleRequest) {
        return ResponseEntity.ok(ageRuleFacade.save(ageRuleRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgeRuleSet> update(@RequestBody AgeRuleRequest ageRuleRequest,
                                             @PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ageRuleFacade.update(ageRuleRequest, id));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        ageRuleFacade.deleteById(id);
    }
}