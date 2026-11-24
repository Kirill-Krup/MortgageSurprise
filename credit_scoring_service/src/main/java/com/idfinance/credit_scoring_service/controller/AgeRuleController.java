package com.idfinance.credit_scoring_service.controller;

import com.idfinance.credit_scoring_service.domain.rule_set.AgeRuleSet;
import com.idfinance.credit_scoring_service.dto.AgeRuleRequest;
import com.idfinance.credit_scoring_service.service.AgeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/age-rule")
public class AgeRuleController {

    private final AgeRuleService ageRuleService;

    @GetMapping
    public ResponseEntity<List<AgeRuleSet>> findAll() {
        return ResponseEntity.ok(ageRuleService.findAll()
                .stream()
                .map(AgeRuleSet::from)
                .collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<AgeRuleSet> save(@RequestBody AgeRuleRequest ageRuleRequest) {
        return ResponseEntity.ok(AgeRuleSet.from(ageRuleService.save(ageRuleRequest)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgeRuleSet> update(@RequestBody AgeRuleRequest ageRuleRequest,
                                             @PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(AgeRuleSet.from(ageRuleService.update(ageRuleRequest, id)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        ageRuleService.deleteById(id);
    }
}