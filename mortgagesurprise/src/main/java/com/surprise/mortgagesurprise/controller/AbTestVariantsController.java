package com.surprise.mortgagesurprise.controller;


import com.surprise.mortgagesurprise.adapter.AbTestVariantsAdapter;
import com.surprise.mortgagesurprise.dto.abtest.AbTestVariantResponse;
import com.surprise.mortgagesurprise.dto.abtest.CreateAbTestVariantRequest;
import com.surprise.mortgagesurprise.dto.abtest.UpdateAbTestVariantRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/ab-test")
@RequiredArgsConstructor
public class AbTestVariantsController {
    private final AbTestVariantsAdapter abTestVariantsAdapter;

    @GetMapping("/variant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Set<AbTestVariantResponse>> findAllByExperimentNameAndVersion(
            @RequestParam String experimentName,
            @RequestParam Integer version
    ) {
        return ResponseEntity.ok(abTestVariantsAdapter.findAllByExperimentNameAndVersion(experimentName, version));
    }

    @PostMapping("/variant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AbTestVariantResponse> save(
            @RequestBody CreateAbTestVariantRequest request
    ) {
        return ResponseEntity.ok(abTestVariantsAdapter.save(request));
    }

    @PutMapping("/variant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Set<AbTestVariantResponse>> updateAll(@RequestBody UpdateAbTestVariantRequest request) {
        return ResponseEntity.ok(abTestVariantsAdapter.updateAll(request));
    }
}