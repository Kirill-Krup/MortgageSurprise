package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.admin.AdminLoanMonitoringDto;
import com.surprise.mortgagesurprise.dto.admin.AdminStatsResponse;
import com.surprise.mortgagesurprise.dto.admin.AdminUserDto;
import com.surprise.mortgagesurprise.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminDashboardService.getPlatformStatistics());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getUsers() {
        return ResponseEntity.ok(adminDashboardService.getAllUsers());
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<AdminUserDto> updateUserStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body
    ) {
        boolean enabled = body.getOrDefault("enabled", true);
        return ResponseEntity.ok(adminDashboardService.updateUserStatus(id, enabled));
    }

    @GetMapping("/loans")
    public ResponseEntity<List<AdminLoanMonitoringDto>> getLoans() {
        return ResponseEntity.ok(adminDashboardService.getAllLoansMonitoring());
    }
}
