package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.profile.PassportDataDTO;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.UserPassportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile/passport")
@RequiredArgsConstructor
public class UserPassportController {

    private final UserPassportService userPassportService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PassportDataDTO> getPassportData(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            return ResponseEntity.notFound().build();
        }
        return userPassportService.getPassportData(userDetails.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PassportDataDTO> savePassportData(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PassportDataDTO dto
    ) {
        if (userDetails == null || userDetails.getId() == null) {
            return ResponseEntity.status(401).build();
        }
        PassportDataDTO saved = userPassportService.saveOrUpdatePassportData(userDetails.getId(), dto);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePassportData(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            return ResponseEntity.status(401).build();
        }
        userPassportService.deletePassportData(userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
