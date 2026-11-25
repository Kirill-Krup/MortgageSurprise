package com.idfinance.credit_scoring_service.controller;

import com.idfinance.credit_scoring_service.facade.GenderRuleFacade;
import com.idfinance.credit_scoring_service.domain.rule_set.GenderRuleSet;
import com.idfinance.credit_scoring_service.dto.GenderRuleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/rules/gender-rules")
public class GenderRuleController {

    private final GenderRuleFacade genderRuleFacade;

    @GetMapping
    public ResponseEntity<Set<GenderRuleSet>> findAll() {
        return ResponseEntity.ok(genderRuleFacade.findAll());
    }

    @PostMapping
    public ResponseEntity<GenderRuleSet> save(@RequestBody GenderRuleRequest genderRuleRequest) {
        return ResponseEntity.ok(genderRuleFacade.save(genderRuleRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenderRuleSet> update(@RequestBody GenderRuleRequest genderRuleRequest,
                                                @PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(genderRuleFacade.update(genderRuleRequest, id));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        genderRuleFacade.deleteById(id);
    }
}