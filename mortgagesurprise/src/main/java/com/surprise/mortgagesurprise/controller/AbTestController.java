package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.adapter.AbTestAdapter;
import com.surprise.mortgagesurprise.dto.abtest.AbTestAssignmentRequest;
import com.surprise.mortgagesurprise.dto.abtest.AbTestAssignmentResponse;
import com.surprise.mortgagesurprise.dto.abtest.AbTestStatsResponse;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ab-test")
@RequiredArgsConstructor
public class AbTestController {

    private final AbTestAdapter abTestAdapter;

    @GetMapping("/variant")
    public ResponseEntity<AbTestAssignmentResponse> getVariant(
            @RequestParam String experimentName,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(abTestAdapter.getOrCreateVariant(userDetails, experimentName));
    }

    @PostMapping("/variant")
    public ResponseEntity<AbTestAssignmentResponse> assignVariant(
            @Valid @RequestBody AbTestAssignmentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(abTestAdapter.getOrCreateVariant(userDetails, request.experimentName()));
    }

    @GetMapping("/stats")
    public ResponseEntity<AbTestStatsResponse> getStats(
            @RequestParam(defaultValue = "mortgage_calculate_button") String experimentName
    ) {
        return ResponseEntity.ok(abTestAdapter.getStats(experimentName));
    }
}
