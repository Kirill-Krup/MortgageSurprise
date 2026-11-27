package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.admin.AdminLoanMonitoringDto;
import com.surprise.mortgagesurprise.dto.admin.AdminStatsResponse;
import com.surprise.mortgagesurprise.dto.admin.AdminUserDto;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.entity.BankOffer;
import com.surprise.mortgagesurprise.model.entity.CalculationScenario;
import com.surprise.mortgagesurprise.model.entity.User;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import com.surprise.mortgagesurprise.repository.jpa.BankOfferRepository;
import com.surprise.mortgagesurprise.repository.jpa.CalculationScenarioRepository;
import com.surprise.mortgagesurprise.repository.jpa.UserRepository;
import com.surprise.mortgagesurprise.repository.mongo.PaymentScheduleMongoRepository;
import com.surprise.mortgagesurprise.repository.mongo.UserActionAuditMongoRepository;
import com.surprise.mortgagesurprise.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final String LOAN_TYPE_MORTGAGE = "MORTGAGE";
    private static final String LOAN_TYPE_MICROLOAN = "MICROLOAN";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PAID_OFF = "PAID_OFF";
    private static final String STATUS_OVERDUE = "OVERDUE";
    private static final String DEFAULT_CURRENCY = "RUB";
    private static final BigDecimal DEFAULT_MORTGAGE_RATE = BigDecimal.valueOf(18.5);
    private static final BigDecimal DEFAULT_MICROLOAN_RATE = BigDecimal.valueOf(0.8);
    private static final int DEFAULT_MICROLOAN_TERM_DAYS = 14;
    private static final int MAX_PAYMENT_DAY_OF_MONTH = 28;
    private static final int SCALE = 2;

    private final UserRepository userRepository;
    private final CalculationScenarioRepository scenarioRepository;
    private final UserActionAuditMongoRepository auditRepository;
    private final PaymentScheduleMongoRepository scheduleRepository;
    private final BankOfferRepository bankOfferRepository;

    @Override
    public AdminStatsResponse getPlatformStatistics() {
        long totalUsers = userRepository.count();
        List<AdminLoanMonitoringDto> allLoans = getAllLoansMonitoring();

        long mortgageCount = allLoans.stream().filter(l -> LOAN_TYPE_MORTGAGE.equals(l.loanType())).count();
        long microloanCount = allLoans.stream().filter(l -> LOAN_TYPE_MICROLOAN.equals(l.loanType())).count();

        BigDecimal mortgagePortfolio = sumAmount(allLoans.stream()
                .filter(l -> LOAN_TYPE_MORTGAGE.equals(l.loanType()))
                .map(AdminLoanMonitoringDto::loanAmount));
        BigDecimal microloanPortfolio = sumAmount(allLoans.stream()
                .filter(l -> LOAN_TYPE_MICROLOAN.equals(l.loanType()))
                .map(AdminLoanMonitoringDto::loanAmount));
        BigDecimal totalPortfolio = mortgagePortfolio.add(microloanPortfolio);

        BigDecimal totalRepaid = sumAmount(allLoans.stream().map(AdminLoanMonitoringDto::paidAmount));
        BigDecimal totalRemaining = sumAmount(allLoans.stream().map(AdminLoanMonitoringDto::remainingBalance));
        BigDecimal totalExpectedRepayment = sumAmount(allLoans.stream().map(AdminLoanMonitoringDto::totalRepayment));

        double repaymentPercent = calculateProgressPercent(totalRepaid, totalExpectedRepayment);
        BigDecimal avgMortgageRate = calculateAverageMortgageRate();

        return new AdminStatsResponse(
                totalUsers,
                allLoans.size(),
                totalPortfolio.setScale(SCALE, RoundingMode.HALF_UP),
                mortgageCount,
                microloanCount,
                mortgagePortfolio.setScale(SCALE, RoundingMode.HALF_UP),
                microloanPortfolio.setScale(SCALE, RoundingMode.HALF_UP),
                totalRepaid.setScale(SCALE, RoundingMode.HALF_UP),
                totalRemaining.setScale(SCALE, RoundingMode.HALF_UP),
                repaymentPercent,
                avgMortgageRate
        );
    }

    @Override
    public List<AdminUserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        Map<UUID, List<AdminLoanMonitoringDto>> loansByUser = getAllLoansMonitoring().stream()
                .filter(l -> l.userId() != null)
                .collect(Collectors.groupingBy(AdminLoanMonitoringDto::userId));

        return users.stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(user -> toAdminUserDto(user, loansByUser.getOrDefault(user.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional
    public AdminUserDto updateUserStatus(UUID userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setEnabled(enabled);
        User saved = userRepository.save(user);

        List<AdminLoanMonitoringDto> userLoans = getLoansForUser(saved);
        return toAdminUserDto(saved, userLoans);
    }

    @Override
    public List<AdminLoanMonitoringDto> getAllLoansMonitoring() {
        Map<UUID, User> userCache = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        List<CalculationScenario> scenarios = scenarioRepository.findAll();
        List<UserActionAuditDocument> microloanAudits = auditRepository
                .findByActionTypeOrderByTimestampDesc(UserAuditActionType.MICROLOAN_APPLICATION);

        return buildLoanMonitoringList(scenarios, microloanAudits, userCache);
    }

    private List<AdminLoanMonitoringDto> getLoansForUser(User user) {
        Map<UUID, User> singleUserCache = Map.of(user.getId(), user);
        List<CalculationScenario> scenarios = scenarioRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<UserActionAuditDocument> microloanAudits = auditRepository
                .findByUserIdOrderByTimestampDesc(user.getId()).stream()
                .filter(doc -> doc.getActionType() == UserAuditActionType.MICROLOAN_APPLICATION)
                .toList();
        return buildLoanMonitoringList(scenarios, microloanAudits, singleUserCache);
    }

    private List<AdminLoanMonitoringDto> buildLoanMonitoringList(
            List<CalculationScenario> scenarios,
            List<UserActionAuditDocument> microloanAudits,
            Map<UUID, User> userCache
    ) {
        LocalDate today = LocalDate.now();
        Map<String, PaymentScheduleDocument> schedulesById = loadSchedulesById(scenarios);
        List<AdminLoanMonitoringDto> result = new ArrayList<>(scenarios.size() + microloanAudits.size());

        for (CalculationScenario scenario : scenarios) {
            result.add(buildMortgageMonitoring(scenario, userCache, schedulesById, today));
        }
        for (UserActionAuditDocument doc : microloanAudits) {
            result.add(buildMicroloanMonitoring(doc, userCache, today));
        }

        result.sort(Comparator.comparing(AdminLoanMonitoringDto::startDate).reversed());
        return result;
    }

    private Map<String, PaymentScheduleDocument> loadSchedulesById(List<CalculationScenario> scenarios) {
        Set<String> scheduleIds = scenarios.stream()
                .map(CalculationScenario::getMongoScheduleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (scheduleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, PaymentScheduleDocument> map = new HashMap<>();
        scheduleRepository.findAllById(scheduleIds)
                .forEach(schedule -> map.put(schedule.getId(), schedule));
        return map;
    }

    private AdminLoanMonitoringDto buildMortgageMonitoring(
            CalculationScenario scenario,
            Map<UUID, User> userCache,
            Map<String, PaymentScheduleDocument> schedulesById,
            LocalDate today
    ) {
        User borrower = userCache.get(scenario.getUserId());
        String borrowerEmail = borrower != null ? borrower.getEmail() : "user-" + scenario.getUserId();
        String borrowerName = resolveBorrowerName(borrower, borrowerEmail);

        Instant startDate = scenario.getCreatedAt() != null ? scenario.getCreatedAt() : Instant.now();
        LocalDate loanStartDate = startDate.atZone(ZoneId.systemDefault()).toLocalDate();

        long monthsElapsed = Math.max(0, ChronoUnit.MONTHS.between(
                loanStartDate.withDayOfMonth(1), today.withDayOfMonth(1)));
        int termMonths = scenario.getTermMonths();
        int elapsedPeriods = (int) Math.min(monthsElapsed, termMonths);
        int remainingPeriods = Math.max(0, termMonths - elapsedPeriods);

        BigDecimal loanAmount = scenario.getLoanAmount() != null ? scenario.getLoanAmount() : BigDecimal.ZERO;
        BigDecimal monthlyPayment = scenario.getMonthlyPayment() != null ? scenario.getMonthlyPayment() : BigDecimal.ZERO;
        BigDecimal totalPayment = scenario.getTotalPayment() != null ? scenario.getTotalPayment() : loanAmount;

        BigDecimal paidAmount = monthlyPayment.multiply(BigDecimal.valueOf(elapsedPeriods)).min(totalPayment);
        BigDecimal remainingBalance = totalPayment.subtract(paidAmount).max(BigDecimal.ZERO);
        BigDecimal principalPaid = resolveMortgagePrincipalPaid(
                scenario, schedulesById, loanAmount, termMonths, elapsedPeriods);

        double progressPercent = calculateProgressPercent(paidAmount, totalPayment);
        String status = remainingPeriods == 0 ? STATUS_PAID_OFF : STATUS_ACTIVE;
        String nextPaymentDate = today.plusMonths(1)
                .withDayOfMonth(Math.min(today.getDayOfMonth(), MAX_PAYMENT_DAY_OF_MONTH))
                .toString();

        return new AdminLoanMonitoringDto(
                "MORT-" + scenario.getId().toString().substring(0, 8).toUpperCase(),
                scenario.getUserId(),
                borrowerEmail,
                borrowerName,
                LOAN_TYPE_MORTGAGE,
                scenario.getTitle() != null ? scenario.getTitle() : "Ипотечный кредит",
                "Ипотечная программа на покупку недвижимости",
                loanAmount.setScale(SCALE, RoundingMode.HALF_UP),
                totalPayment.setScale(SCALE, RoundingMode.HALF_UP),
                scenario.getAnnualInterestRate() != null ? scenario.getAnnualInterestRate() : DEFAULT_MORTGAGE_RATE,
                (termMonths / 12) + " лет (" + termMonths + " мес.)",
                termMonths,
                scenario.getCurrency() != null ? scenario.getCurrency().name() : DEFAULT_CURRENCY,
                startDate,
                elapsedPeriods,
                remainingPeriods,
                paidAmount.setScale(SCALE, RoundingMode.HALF_UP),
                principalPaid.setScale(SCALE, RoundingMode.HALF_UP),
                remainingBalance.setScale(SCALE, RoundingMode.HALF_UP),
                progressPercent,
                monthlyPayment.setScale(SCALE, RoundingMode.HALF_UP),
                status,
                nextPaymentDate,
                monthlyPayment.setScale(SCALE, RoundingMode.HALF_UP),
                scenario.getMongoScheduleId()
        );
    }

    private BigDecimal resolveMortgagePrincipalPaid(
            CalculationScenario scenario,
            Map<String, PaymentScheduleDocument> schedulesById,
            BigDecimal loanAmount,
            int termMonths,
            int elapsedPeriods
    ) {
        if (elapsedPeriods > 0 && scenario.getMongoScheduleId() != null) {
            PaymentScheduleDocument schedule = schedulesById.get(scenario.getMongoScheduleId());
            if (schedule != null && schedule.getItems() != null && !schedule.getItems().isEmpty()) {
                var items = schedule.getItems();
                int idx = Math.min(elapsedPeriods, items.size());
                BigDecimal closingBalance = items.get(idx - 1).closingBalance();
                if (closingBalance != null) {
                    return loanAmount.subtract(closingBalance).max(BigDecimal.ZERO);
                }
            }
        }
        if (termMonths > 0) {
            return loanAmount.multiply(BigDecimal.valueOf(elapsedPeriods))
                    .divide(BigDecimal.valueOf(termMonths), SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private AdminLoanMonitoringDto buildMicroloanMonitoring(
            UserActionAuditDocument doc,
            Map<UUID, User> userCache,
            LocalDate today
    ) {
        Instant startDate = doc.getTimestamp() != null ? doc.getTimestamp() : Instant.now();
        LocalDate loanStartDate = startDate.atZone(ZoneId.systemDefault()).toLocalDate();

        int termDays = doc.getTermDays() != null ? doc.getTermDays() : DEFAULT_MICROLOAN_TERM_DAYS;
        long daysElapsed = Math.max(0, ChronoUnit.DAYS.between(loanStartDate, today));
        int elapsedPeriods = (int) Math.min(daysElapsed, termDays);
        int remainingPeriods = Math.max(0, termDays - (int) daysElapsed);

        BigDecimal loanAmount = doc.getLoanAmount() != null ? doc.getLoanAmount() : BigDecimal.ZERO;
        BigDecimal totalPayment = doc.getTotalPayment() != null ? doc.getTotalPayment() : loanAmount;
        BigDecimal dailyPayment = termDays > 0
                ? totalPayment.divide(BigDecimal.valueOf(termDays), SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal paidAmount = daysElapsed >= termDays
                ? totalPayment
                : dailyPayment.multiply(BigDecimal.valueOf(elapsedPeriods));
        BigDecimal remainingBalance = totalPayment.subtract(paidAmount).max(BigDecimal.ZERO);
        BigDecimal principalPaid = termDays > 0
                ? loanAmount.multiply(BigDecimal.valueOf(elapsedPeriods))
                .divide(BigDecimal.valueOf(termDays), SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        double progressPercent = calculateProgressPercent(paidAmount, totalPayment);
        String status = resolveMicroloanStatus(doc, daysElapsed, termDays, remainingPeriods);

        String appId = doc.getApplicationId() != null
                ? doc.getApplicationId()
                : ("ML-" + doc.getId().substring(0, 6));
        String typeTitle = "VANITY".equalsIgnoreCase(doc.getMicroloanType())
                ? "Роскошный максимум"
                : "Базовый минимум";
        String scheduleId = doc.getMetadata() != null && doc.getMetadata().get("scheduleId") != null
                ? String.valueOf(doc.getMetadata().get("scheduleId"))
                : null;

        User borrower = doc.getUserId() != null ? userCache.get(doc.getUserId()) : null;
        String fallbackEmail = doc.getUserEmail() != null ? doc.getUserEmail() : "unknown@user.com";
        String borrowerName = borrower != null || doc.getUserEmail() != null
                ? resolveBorrowerName(borrower, fallbackEmail)
                : "Borrower";

        return new AdminLoanMonitoringDto(
                appId,
                doc.getUserId(),
                fallbackEmail,
                borrowerName,
                LOAN_TYPE_MICROLOAN,
                typeTitle,
                doc.getLoanPurpose() != null ? doc.getLoanPurpose() : "Срочные потребительские нужды",
                loanAmount.setScale(SCALE, RoundingMode.HALF_UP),
                totalPayment.setScale(SCALE, RoundingMode.HALF_UP),
                DEFAULT_MICROLOAN_RATE,
                termDays + " дн.",
                termDays,
                doc.getCurrency() != null ? doc.getCurrency().name() : DEFAULT_CURRENCY,
                startDate,
                elapsedPeriods,
                remainingPeriods,
                paidAmount.setScale(SCALE, RoundingMode.HALF_UP),
                principalPaid.setScale(SCALE, RoundingMode.HALF_UP),
                remainingBalance.setScale(SCALE, RoundingMode.HALF_UP),
                progressPercent,
                dailyPayment.setScale(SCALE, RoundingMode.HALF_UP),
                status,
                today.plusDays(1).toString(),
                dailyPayment.setScale(SCALE, RoundingMode.HALF_UP),
                scheduleId
        );
    }

    private String resolveMicroloanStatus(
            UserActionAuditDocument doc,
            long daysElapsed,
            int termDays,
            int remainingPeriods
    ) {
        String metaStatus = doc.getMetadata() != null && doc.getMetadata().get("status") != null
                ? String.valueOf(doc.getMetadata().get("status"))
                : null;
        if (STATUS_PAID_OFF.equalsIgnoreCase(metaStatus)) {
            return STATUS_PAID_OFF;
        }
        if (daysElapsed > termDays) {
            return STATUS_OVERDUE;
        }
        return remainingPeriods == 0 ? STATUS_PAID_OFF : STATUS_ACTIVE;
    }

    private AdminUserDto toAdminUserDto(User user, List<AdminLoanMonitoringDto> userLoans) {
        BigDecimal totalDebt = sumAmount(userLoans.stream().map(AdminLoanMonitoringDto::loanAmount));
        BigDecimal remainingDebt = sumAmount(userLoans.stream().map(AdminLoanMonitoringDto::remainingBalance));
        return new AdminUserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                userLoans.size(),
                totalDebt.setScale(SCALE, RoundingMode.HALF_UP),
                remainingDebt.setScale(SCALE, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal calculateAverageMortgageRate() {
        List<BankOffer> offers = bankOfferRepository.findByActiveTrue();
        if (offers.isEmpty()) {
            return DEFAULT_MORTGAGE_RATE;
        }
        BigDecimal sumRate = sumAmount(offers.stream()
                .map(BankOffer::getAnnualInterestRate)
                .filter(Objects::nonNull));
        return sumRate.divide(BigDecimal.valueOf(offers.size()), SCALE, RoundingMode.HALF_UP);
    }

    private double calculateProgressPercent(BigDecimal paid, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        double percent = paid.multiply(BigDecimal.valueOf(100))
                .divide(total, SCALE, RoundingMode.HALF_UP)
                .doubleValue();
        return Math.min(100.0, Math.max(0.0, percent));
    }

    private String resolveBorrowerName(User borrower, String fallbackEmail) {
        if (borrower != null && borrower.getFirstName() != null) {
            String lastName = borrower.getLastName() != null ? borrower.getLastName() : "";
            return (borrower.getFirstName() + " " + lastName).trim();
        }
        return fallbackEmail.split("@")[0];
    }

    private BigDecimal sumAmount(java.util.stream.Stream<BigDecimal> amounts) {
        return amounts.reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

