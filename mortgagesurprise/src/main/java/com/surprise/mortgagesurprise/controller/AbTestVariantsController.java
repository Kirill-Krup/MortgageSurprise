package com.surprise.mortgagesurprise.controller;


import com.surprise.mortgagesurprise.adapter.AbTestVariantsAdapter;
import com.surprise.mortgagesurprise.dto.abtest.AbTestVariantResponse;
import com.surprise.mortgagesurprise.dto.abtest.CreateAbTestVariantRequest;
import com.surprise.mortgagesurprise.dto.abtest.UpdateAbTestVariantRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/ab-test/variants")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AbTestVariantsController {
    private final AbTestVariantsAdapter abTestVariantsAdapter;

    @GetMapping
    public ResponseEntity<Set<AbTestVariantResponse>> findAllByExperimentNameAndVersion(
            @RequestParam String experimentName,
            @RequestParam Integer version
    ) {
        return ResponseEntity.ok(abTestVariantsAdapter.findAllByExperimentNameAndVersion(experimentName, version));
    }

    @PostMapping
    public ResponseEntity<AbTestVariantResponse> save(
            @Valid @RequestBody CreateAbTestVariantRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(abTestVariantsAdapter.save(request));
    }

    @PutMapping
    public ResponseEntity<Set<AbTestVariantResponse>> updateAll(
            @Valid @RequestBody UpdateAbTestVariantRequest request
    ) {
        return ResponseEntity.ok(abTestVariantsAdapter.updateAll(request));
    }
}
