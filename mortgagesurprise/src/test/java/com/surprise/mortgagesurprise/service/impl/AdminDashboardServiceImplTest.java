package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.admin.AdminLoanMonitoringDto;
import com.surprise.mortgagesurprise.dto.admin.AdminStatsResponse;
import com.surprise.mortgagesurprise.dto.admin.AdminUserDto;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.entity.BankOffer;
import com.surprise.mortgagesurprise.model.entity.CalculationScenario;
import com.surprise.mortgagesurprise.model.entity.Role;
import com.surprise.mortgagesurprise.model.entity.User;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import com.surprise.mortgagesurprise.repository.jpa.BankOfferRepository;
import com.surprise.mortgagesurprise.repository.jpa.CalculationScenarioRepository;
import com.surprise.mortgagesurprise.repository.jpa.UserRepository;
import com.surprise.mortgagesurprise.repository.mongo.PaymentScheduleMongoRepository;
import com.surprise.mortgagesurprise.repository.mongo.UserActionAuditMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CalculationScenarioRepository scenarioRepository;

    @Mock
    private UserActionAuditMongoRepository auditRepository;

    @Mock
    private PaymentScheduleMongoRepository scheduleRepository;

    @Mock
    private BankOfferRepository bankOfferRepository;

    @InjectMocks
    private AdminDashboardServiceImpl service;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User("client@bank.com", "pass123", "Ivan", "Petrov", Role.ROLE_USER);
        testUser.setId(userId);
    }

    @Test
    @DisplayName("getPlatformStatistics returns aggregated real data from DB")
    void getPlatformStatistics_ReturnsRealAggregatedData() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(scenarioRepository.findAll()).thenReturn(List.of());
        when(auditRepository.findByActionTypeOrderByTimestampDesc(UserAuditActionType.MICROLOAN_APPLICATION))
                .thenReturn(List.of());
        when(bankOfferRepository.findByActiveTrue()).thenReturn(List.of());

        AdminStatsResponse stats = service.getPlatformStatistics();

        assertThat(stats.totalUsers()).isEqualTo(10L);
        assertThat(stats.activeLoansCount()).isEqualTo(0L);
        assertThat(stats.totalPortfolioAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getAllUsers calculates loans and debt per user correctly")
    void getAllUsers_ReturnsEnrichedUserList() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(scenarioRepository.findAll()).thenReturn(List.of());
        when(auditRepository.findByActionTypeOrderByTimestampDesc(UserAuditActionType.MICROLOAN_APPLICATION))
                .thenReturn(List.of());

        List<AdminUserDto> users = service.getAllUsers();

        assertThat(users).hasSize(1);
        AdminUserDto dto = users.get(0);
        assertThat(dto.email()).isEqualTo("client@bank.com");
        assertThat(dto.enabled()).isTrue();
        assertThat(dto.activeLoansCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("updateUserStatus updates user enabled flag")
    void updateUserStatus_ModifiesAndSavesUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(scenarioRepository.findAll()).thenReturn(List.of());
        when(auditRepository.findByActionTypeOrderByTimestampDesc(UserAuditActionType.MICROLOAN_APPLICATION))
                .thenReturn(List.of());

        AdminUserDto updated = service.updateUserStatus(userId, false);

        assertThat(updated.enabled()).isFalse();
    }

    @Test
    @DisplayName("getAllLoansMonitoring aggregates mortgage scenarios and microloans")
    void getAllLoansMonitoring_AggregatesBothLoanTypes() {
        CalculationScenario scenario = new CalculationScenario(
                userId, "Первичка 20 лет", BigDecimal.valueOf(5000000), BigDecimal.valueOf(1000000),
                BigDecimal.valueOf(4000000), 240, BigDecimal.valueOf(18.5), PaymentType.ANNUITY,
                Currency.RUB, BigDecimal.valueOf(62000), BigDecimal.valueOf(10880000),
                BigDecimal.valueOf(14880000), null
        );
        scenario.setId(UUID.randomUUID());
        scenario.setCreatedAt(Instant.now().minus(60, ChronoUnit.DAYS));

        UserActionAuditDocument microloanDoc = new UserActionAuditDocument(
                "audit-1", userId, "client@bank.com", UserAuditActionType.MICROLOAN_APPLICATION,
                "Microloan 30000", null, null, null,
                BigDecimal.valueOf(30000), null, null, null, 14,
                BigDecimal.valueOf(0.8), null, BigDecimal.valueOf(33360), null,
                "BASIC_NEEDS", "Ремонт", "MS-99881", Currency.RUB, null, Instant.now().minus(7, ChronoUnit.DAYS)
        );

        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(scenarioRepository.findAll()).thenReturn(List.of(scenario));
        when(auditRepository.findByActionTypeOrderByTimestampDesc(UserAuditActionType.MICROLOAN_APPLICATION))
                .thenReturn(List.of(microloanDoc));

        List<AdminLoanMonitoringDto> loans = service.getAllLoansMonitoring();

        assertThat(loans).hasSize(2);
        AdminLoanMonitoringDto ml = loans.stream().filter(l -> "MICROLOAN".equals(l.loanType())).findFirst().orElseThrow();
        assertThat(ml.id()).isEqualTo("MS-99881");
        assertThat(ml.elapsedPeriods()).isGreaterThanOrEqualTo(6);
        assertThat(ml.progressPercent()).isGreaterThan(0.0);
    }
}
