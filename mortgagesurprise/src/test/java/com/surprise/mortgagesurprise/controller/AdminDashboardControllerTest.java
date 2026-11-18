package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.admin.AdminLoanMonitoringDto;
import com.surprise.mortgagesurprise.dto.admin.AdminStatsResponse;
import com.surprise.mortgagesurprise.dto.admin.AdminUserDto;
import com.surprise.mortgagesurprise.service.AdminDashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    @Mock
    private AdminDashboardService adminDashboardService;

    @InjectMocks
    private AdminDashboardController controller;

    @Test
    @DisplayName("GET /api/v1/admin/stats returns real stats from service")
    void getStats_ReturnsOk() {
        AdminStatsResponse mockStats = new AdminStatsResponse(
                15, 3, BigDecimal.valueOf(1500000), 2, 1,
                BigDecimal.valueOf(1400000), BigDecimal.valueOf(100000),
                BigDecimal.valueOf(300000), BigDecimal.valueOf(1200000),
                20.0, BigDecimal.valueOf(18.5)
        );
        when(adminDashboardService.getPlatformStatistics()).thenReturn(mockStats);

        ResponseEntity<AdminStatsResponse> response = controller.getStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalUsers()).isEqualTo(15);
        assertThat(response.getBody().repaymentProgressPercent()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("GET /api/v1/admin/users returns user list")
    void getUsers_ReturnsOk() {
        UUID userId = UUID.randomUUID();
        AdminUserDto userDto = new AdminUserDto(
                userId, "user@test.com", "Test", "User", "ROLE_USER",
                true, true, Instant.now(), 1, BigDecimal.valueOf(500000), BigDecimal.valueOf(400000)
        );
        when(adminDashboardService.getAllUsers()).thenReturn(List.of(userDto));

        ResponseEntity<List<AdminUserDto>> response = controller.getUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(1);
        assertThat(response.getBody().get(0).email()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/status updates user status")
    void updateUserStatus_ReturnsUpdatedUser() {
        UUID userId = UUID.randomUUID();
        AdminUserDto blockedDto = new AdminUserDto(
                userId, "user@test.com", "Test", "User", "ROLE_USER",
                false, true, Instant.now(), 0, BigDecimal.ZERO, BigDecimal.ZERO
        );
        when(adminDashboardService.updateUserStatus(userId, false)).thenReturn(blockedDto);

        ResponseEntity<AdminUserDto> response = controller.updateUserStatus(userId, Map.of("enabled", false));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().enabled()).isFalse();
        verify(adminDashboardService).updateUserStatus(userId, false);
    }

    @Test
    @DisplayName("GET /api/v1/admin/loans returns loan monitoring list")
    void getLoans_ReturnsOk() {
        AdminLoanMonitoringDto loanDto = new AdminLoanMonitoringDto(
                "MS-12345", UUID.randomUUID(), "borrower@test.com", "Borrower",
                "MICROLOAN", "Базовый минимум", "Ремонт",
                BigDecimal.valueOf(50000), BigDecimal.valueOf(55000), BigDecimal.valueOf(0.8),
                "14 дн.", 14, "RUB", Instant.now(), 7, 7,
                BigDecimal.valueOf(27500), BigDecimal.valueOf(25000), BigDecimal.valueOf(27500),
                50.0, BigDecimal.valueOf(3928.57), "ACTIVE", "2026-11-19", BigDecimal.valueOf(3928.57), null
        );
        when(adminDashboardService.getAllLoansMonitoring()).thenReturn(List.of(loanDto));

        ResponseEntity<List<AdminLoanMonitoringDto>> response = controller.getLoans();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(1);
        assertThat(response.getBody().get(0).id()).isEqualTo("MS-12345");
        assertThat(response.getBody().get(0).progressPercent()).isEqualTo(50.0);
    }
}
