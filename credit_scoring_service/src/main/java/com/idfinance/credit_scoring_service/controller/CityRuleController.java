package com.idfinance.credit_scoring_service.controller;

import com.idfinance.credit_scoring_service.domain.rule_set.CityRuleSet;
import com.idfinance.credit_scoring_service.dto.CityRuleRequest;
import com.idfinance.credit_scoring_service.service.CityRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/city-rule")
public class CityRuleController {

    private final CityRuleService cityRuleService;

    @GetMapping
    public ResponseEntity<List<CityRuleSet>> findAll() {
        return ResponseEntity.ok(cityRuleService.findAll()
                .stream()
                .map(CityRuleSet::from)
                .collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<CityRuleSet> save(@RequestBody CityRuleRequest cityRuleRequest) {
        return ResponseEntity.ok(CityRuleSet.from(cityRuleService.save(cityRuleRequest)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CityRuleSet> update(@RequestBody CityRuleRequest cityRuleRequest,
                                              @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(CityRuleSet.from(cityRuleService.update(cityRuleRequest, id)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        cityRuleService.deleteById(id);
    }
}