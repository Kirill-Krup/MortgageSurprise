package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.audit.BankOfferSelectionRequest;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.UserAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if (userDetails != null && !isAdmin(userDetails)) {
            userAuditService.auditBankOfferSelected(userDetails.getId(), userDetails.getUsername(), false, request);
        }
        return ResponseEntity.ok(Map.of("success", true, "logged", userDetails != null && !isAdmin(userDetails)));
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
        List<UserActionAuditDocument> audits = userAuditService.getAuditsForUser(userDetails.getId());
        if (audits == null) {
            audits = List.of();
        }
        if (action != null) {
            final UserAuditActionType canonicalTarget = action.canonical();
            audits = audits.stream()
                    .filter(a -> a != null && a.getActionType() != null &&
                            (a.getActionType() == action || a.getActionType().canonical() == canonicalTarget))
                    .toList();
        }
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/admin/audit/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserActionAuditDocument>> getAdminAuditLogs(
            @RequestParam(required = false) UserAuditActionType action
    ) {
        List<UserActionAuditDocument> audits;
        if (action != null) {
            audits = userAuditService.getAuditsByAction(action);
        } else {
            audits = userAuditService.getAllAudits();
        }
        return ResponseEntity.ok(audits != null ? audits : List.of());
    }

    private boolean isAdmin(CustomUserDetails userDetails) {
        return userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
