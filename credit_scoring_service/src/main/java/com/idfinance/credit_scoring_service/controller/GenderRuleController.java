package com.idfinance.credit_scoring_service.controller;

import com.idfinance.credit_scoring_service.domain.rule_set.GenderRuleSet;
import com.idfinance.credit_scoring_service.dto.GenderRuleRequest;
import com.idfinance.credit_scoring_service.service.GenderRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/gender-rule")
public class GenderRuleController {

    private final GenderRuleService genderRuleService;

    @GetMapping
    public ResponseEntity<List<GenderRuleSet>> findAll() {
        return ResponseEntity.ok(genderRuleService.findAll()
                .stream()
                .map(GenderRuleSet::from)
                .collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<GenderRuleSet> save(@RequestBody GenderRuleRequest genderRuleRequest) {
        return ResponseEntity.ok(GenderRuleSet.from(genderRuleService.save(genderRuleRequest)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenderRuleSet> update(@RequestBody GenderRuleRequest genderRuleRequest,
                                                @PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(GenderRuleSet.from(genderRuleService.update(genderRuleRequest, id)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        genderRuleService.deleteById(id);
    }
}