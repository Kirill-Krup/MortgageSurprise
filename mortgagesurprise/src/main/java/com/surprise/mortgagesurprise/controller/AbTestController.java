package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.abtest.AbTestAssignmentRequest;
import com.surprise.mortgagesurprise.dto.abtest.AbTestAssignmentResponse;
import com.surprise.mortgagesurprise.dto.abtest.AbTestStatsResponse;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.AbTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ab-test")
@RequiredArgsConstructor
public class AbTestController {

    private final AbTestService abTestService;

    @GetMapping("/variant")
    public ResponseEntity<AbTestAssignmentResponse> getVariant(
            @RequestParam String experimentName,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(abTestService.getOrCreateVariant(userId, experimentName));
    }

    @PostMapping("/variant")
    public ResponseEntity<AbTestAssignmentResponse> assignVariant(
            @Valid @RequestBody AbTestAssignmentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(abTestService.getOrCreateVariant(userId, request.experimentName()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<AbTestStatsResponse> getStats(
            @RequestParam(defaultValue = "mortgage_calculate_button") String experimentName
    ) {
        return ResponseEntity.ok(abTestService.getStats(experimentName));
    }
}
