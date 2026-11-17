package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.audit.BankOfferSelectionRequest;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.UserAuditService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class UserAuditController {

    private final UserAuditService userAuditService;

    public UserAuditController(UserAuditService userAuditService) {
        this.userAuditService = userAuditService;
    }

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

    @GetMapping("/admin/audit/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserActionAuditDocument>> getAdminAuditLogs(
            @RequestParam(required = false) UserAuditActionType action
    ) {
        if (action != null) {
            return ResponseEntity.ok(userAuditService.getAuditsByAction(action));
        }
        return ResponseEntity.ok(userAuditService.getAllAudits());
    }

    private boolean isAdmin(CustomUserDetails userDetails) {
        return userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
