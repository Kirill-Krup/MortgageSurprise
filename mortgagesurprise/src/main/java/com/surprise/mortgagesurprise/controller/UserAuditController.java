package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.audit.BankOfferSelectionRequest;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.UserAuditService;
import com.surprise.mortgagesurprise.util.CredentialValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserAuditController {

    private final UserAuditService userAuditService;

    @PostMapping("/audit/bank-offer-selected")
    public ResponseEntity<Map<String, Object>> logBankOfferSelected(
            @Valid @RequestBody BankOfferSelectionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean shouldLog = userDetails != null && !CredentialValidator.isAdmin(userDetails);
        if (shouldLog) {
            userAuditService.auditBankOfferSelected(userDetails.getId(), userDetails.getUsername(), false, request);
        }
        return ResponseEntity.ok(Map.of("success", true, "logged", shouldLog));
    }

    @GetMapping("/profile/activity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserActionAuditDocument>> getMyActivity(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) UserAuditActionType action
    ) {
        if (userDetails == null || userDetails.getId() == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(userAuditService.getAuditsForUser(userDetails.getId(), action));
    }

    @GetMapping("/admin/audit/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserActionAuditDocument>> getAdminAuditLogs(
            @RequestParam(required = false) UserAuditActionType action
    ) {
        List<UserActionAuditDocument> audits = action != null
                ? userAuditService.getAuditsByAction(action)
                : userAuditService.getAllAudits();
        return ResponseEntity.ok(audits != null ? audits : List.of());
    }
}
