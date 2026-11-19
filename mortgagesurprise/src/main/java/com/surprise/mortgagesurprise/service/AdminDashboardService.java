package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.admin.AdminLoanMonitoringDto;
import com.surprise.mortgagesurprise.dto.admin.AdminStatsResponse;
import com.surprise.mortgagesurprise.dto.admin.AdminUserDto;

import java.util.List;
import java.util.UUID;

public interface AdminDashboardService {

    AdminStatsResponse getPlatformStatistics();

    List<AdminUserDto> getAllUsers();

    AdminUserDto updateUserStatus(UUID userId, boolean enabled);

    List<AdminLoanMonitoringDto> getAllLoansMonitoring();
}
