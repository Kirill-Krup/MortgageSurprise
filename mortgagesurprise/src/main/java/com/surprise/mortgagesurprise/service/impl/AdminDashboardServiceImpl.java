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
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final CalculationScenarioRepository scenarioRepository;
    private final UserActionAuditMongoRepository auditRepository;
    private final PaymentScheduleMongoRepository scheduleRepository;
    private final BankOfferRepository bankOfferRepository;

    @Override
    public AdminStatsResponse getPlatformStatistics() {
        long totalUsers = userRepository.count();

        List<AdminLoanMonitoringDto> allLoans = getAllLoansMonitoring();
        long activeLoansCount = allLoans.size();

        long mortgageCount = allLoans.stream().filter(l -> "MORTGAGE".equals(l.loanType())).count();
        long microloanCount = allLoans.stream().filter(l -> "MICROLOAN".equals(l.loanType())).count();

        BigDecimal mortgagePortfolio = allLoans.stream()
                .filter(l -> "MORTGAGE".equals(l.loanType()))
                .map(AdminLoanMonitoringDto::loanAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal microloanPortfolio = allLoans.stream()
                .filter(l -> "MICROLOAN".equals(l.loanType()))
                .map(AdminLoanMonitoringDto::loanAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPortfolio = mortgagePortfolio.add(microloanPortfolio);

        BigDecimal totalRepaid = allLoans.stream()
                .map(AdminLoanMonitoringDto::paidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemaining = allLoans.stream()
                .map(AdminLoanMonitoringDto::remainingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpectedRepayment = allLoans.stream()
                .map(AdminLoanMonitoringDto::totalRepayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double repaymentPercent = 0.0;
        if (totalExpectedRepayment.compareTo(BigDecimal.ZERO) > 0) {
            repaymentPercent = totalRepaid.multiply(BigDecimal.valueOf(100))
                    .divide(totalExpectedRepayment, 2, RoundingMode.HALF_UP)
                    .doubleValue();
            repaymentPercent = Math.min(100.0, Math.max(0.0, repaymentPercent));
        }

        List<BankOffer> offers = bankOfferRepository.findByActiveTrue();
        BigDecimal avgMortgageRate = BigDecimal.valueOf(18.5);
        if (!offers.isEmpty()) {
            BigDecimal sumRate = offers.stream()
                    .map(BankOffer::getAnnualInterestRate)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            avgMortgageRate = sumRate.divide(BigDecimal.valueOf(offers.size()), 2, RoundingMode.HALF_UP);
        }

        return new AdminStatsResponse(
                totalUsers,
                activeLoansCount,
                totalPortfolio.setScale(2, RoundingMode.HALF_UP),
                mortgageCount,
                microloanCount,
                mortgagePortfolio.setScale(2, RoundingMode.HALF_UP),
                microloanPortfolio.setScale(2, RoundingMode.HALF_UP),
                totalRepaid.setScale(2, RoundingMode.HALF_UP),
                totalRemaining.setScale(2, RoundingMode.HALF_UP),
                repaymentPercent,
                avgMortgageRate
        );
    }

    @Override
    public List<AdminUserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<AdminLoanMonitoringDto> allLoans = getAllLoansMonitoring();

        Map<UUID, List<AdminLoanMonitoringDto>> loansByUser = allLoans.stream()
                .filter(l -> l.userId() != null)
                .collect(Collectors.groupingBy(AdminLoanMonitoringDto::userId));

        return users.stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(user -> {
                    List<AdminLoanMonitoringDto> userLoans = loansByUser.getOrDefault(user.getId(), List.of());
                    BigDecimal totalDebt = userLoans.stream()
                            .map(AdminLoanMonitoringDto::loanAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal remainingDebt = userLoans.stream()
                            .map(AdminLoanMonitoringDto::remainingBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

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
                            totalDebt.setScale(2, RoundingMode.HALF_UP),
                            remainingDebt.setScale(2, RoundingMode.HALF_UP)
                    );
                })
                .toList();
    }

    @Override
    @Transactional
    public AdminUserDto updateUserStatus(UUID userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setEnabled(enabled);
        User saved = userRepository.save(user);

        List<AdminLoanMonitoringDto> allLoans = getAllLoansMonitoring();
        List<AdminLoanMonitoringDto> userLoans = allLoans.stream()
                .filter(l -> userId.equals(l.userId()))
                .toList();

        BigDecimal totalDebt = userLoans.stream()
                .map(AdminLoanMonitoringDto::loanAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingDebt = userLoans.stream()
                .map(AdminLoanMonitoringDto::remainingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AdminUserDto(
                saved.getId(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getRole().name(),
                saved.isEnabled(),
                saved.isEmailVerified(),
                saved.getCreatedAt(),
                userLoans.size(),
                totalDebt.setScale(2, RoundingMode.HALF_UP),
                remainingDebt.setScale(2, RoundingMode.HALF_UP)
        );
    }

    @Override
    public List<AdminLoanMonitoringDto> getAllLoansMonitoring() {
        List<AdminLoanMonitoringDto> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        Map<UUID, User> userCache = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        // 1. Mortgages from CalculationScenario
        List<CalculationScenario> scenarios = scenarioRepository.findAll();
        for (CalculationScenario scenario : scenarios) {
            User borrower = userCache.get(scenario.getUserId());
            String borrowerEmail = borrower != null ? borrower.getEmail() : "user-" + scenario.getUserId();
            String borrowerName = (borrower != null && borrower.getFirstName() != null)
                    ? (borrower.getFirstName() + " " + (borrower.getLastName() != null ? borrower.getLastName() : "")).trim()
                    : borrowerEmail.split("@")[0];

            Instant startDate = scenario.getCreatedAt() != null ? scenario.getCreatedAt() : Instant.now();
            LocalDate loanStartDate = startDate.atZone(ZoneId.systemDefault()).toLocalDate();

            long monthsElapsed = ChronoUnit.MONTHS.between(loanStartDate.withDayOfMonth(1), today.withDayOfMonth(1));
            if (monthsElapsed < 0) {
                monthsElapsed = 0;
            }
            int termMonths = scenario.getTermMonths();
            int elapsedPeriods = (int) Math.min(monthsElapsed, termMonths);
            int remainingPeriods = Math.max(0, termMonths - elapsedPeriods);

            BigDecimal loanAmount = scenario.getLoanAmount() != null ? scenario.getLoanAmount() : BigDecimal.ZERO;
            BigDecimal monthlyPayment = scenario.getMonthlyPayment() != null ? scenario.getMonthlyPayment() : BigDecimal.ZERO;
            BigDecimal totalPayment = scenario.getTotalPayment() != null ? scenario.getTotalPayment() : loanAmount;

            BigDecimal paidAmount = monthlyPayment.multiply(BigDecimal.valueOf(elapsedPeriods));
            if (paidAmount.compareTo(totalPayment) > 0) {
                paidAmount = totalPayment;
            }
            BigDecimal remainingBalance = totalPayment.subtract(paidAmount).max(BigDecimal.ZERO);

            BigDecimal principalPaid = BigDecimal.ZERO;
            if (scenario.getMongoScheduleId() != null) {
                Optional<PaymentScheduleDocument> schedOpt = scheduleRepository.findById(scenario.getMongoScheduleId());
                if (schedOpt.isPresent() && schedOpt.get().getItems() != null && !schedOpt.get().getItems().isEmpty()) {
                    var items = schedOpt.get().getItems();
                    if (elapsedPeriods > 0) {
                        int idx = Math.min(elapsedPeriods, items.size());
                        var currentItem = items.get(idx - 1);
                        if (currentItem.closingBalance() != null) {
                            BigDecimal closingBalance = currentItem.closingBalance();
                            principalPaid = loanAmount.subtract(closingBalance).max(BigDecimal.ZERO);
                        }
                    }
                }
            }
            if (principalPaid.compareTo(BigDecimal.ZERO) == 0 && termMonths > 0) {
                principalPaid = loanAmount.multiply(BigDecimal.valueOf(elapsedPeriods))
                        .divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
            }

            double progressPercent = 0.0;
            if (totalPayment.compareTo(BigDecimal.ZERO) > 0) {
                progressPercent = paidAmount.multiply(BigDecimal.valueOf(100))
                        .divide(totalPayment, 2, RoundingMode.HALF_UP)
                        .doubleValue();
                progressPercent = Math.min(100.0, Math.max(0.0, progressPercent));
            }

            String status = remainingPeriods == 0 ? "PAID_OFF" : "ACTIVE";
            String nextPaymentDate = today.plusMonths(1).withDayOfMonth(Math.min(today.getDayOfMonth(), 28)).toString();

            result.add(new AdminLoanMonitoringDto(
                    "MORT-" + scenario.getId().toString().substring(0, 8).toUpperCase(),
                    scenario.getUserId(),
                    borrowerEmail,
                    borrowerName,
                    "MORTGAGE",
                    scenario.getTitle() != null ? scenario.getTitle() : "Ипотечный кредит",
                    "Ипотечная программа на покупку недвижимости",
                    loanAmount.setScale(2, RoundingMode.HALF_UP),
                    totalPayment.setScale(2, RoundingMode.HALF_UP),
                    scenario.getAnnualInterestRate() != null ? scenario.getAnnualInterestRate() : BigDecimal.valueOf(18.5),
                    (termMonths / 12) + " лет (" + termMonths + " мес.)",
                    termMonths,
                    scenario.getCurrency() != null ? scenario.getCurrency().name() : "RUB",
                    startDate,
                    elapsedPeriods,
                    remainingPeriods,
                    paidAmount.setScale(2, RoundingMode.HALF_UP),
                    principalPaid.setScale(2, RoundingMode.HALF_UP),
                    remainingBalance.setScale(2, RoundingMode.HALF_UP),
                    progressPercent,
                    monthlyPayment.setScale(2, RoundingMode.HALF_UP),
                    status,
                    nextPaymentDate,
                    monthlyPayment.setScale(2, RoundingMode.HALF_UP),
                    scenario.getMongoScheduleId()
            ));
        }

        // 2. Microloans from UserActionAuditMongoRepository (MICROLOAN_APPLICATION)
        List<UserActionAuditDocument> microloanAudits = auditRepository
                .findByActionTypeOrderByTimestampDesc(UserAuditActionType.MICROLOAN_APPLICATION);

        for (UserActionAuditDocument doc : microloanAudits) {
            Instant startDate = doc.getTimestamp() != null ? doc.getTimestamp() : Instant.now();
            LocalDate loanStartDate = startDate.atZone(ZoneId.systemDefault()).toLocalDate();

            int termDays = doc.getTermDays() != null ? doc.getTermDays() : 14;
            long daysElapsed = ChronoUnit.DAYS.between(loanStartDate, today);
            if (daysElapsed < 0) {
                daysElapsed = 0;
            }

            int elapsedPeriods = (int) Math.min(daysElapsed, termDays);
            int remainingPeriods = Math.max(0, termDays - (int) daysElapsed);

            BigDecimal loanAmount = doc.getLoanAmount() != null ? doc.getLoanAmount() : BigDecimal.ZERO;
            BigDecimal totalPayment = doc.getTotalPayment() != null ? doc.getTotalPayment() : loanAmount;

            BigDecimal dailyPayment = termDays > 0
                    ? totalPayment.divide(BigDecimal.valueOf(termDays), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal paidAmount = dailyPayment.multiply(BigDecimal.valueOf(elapsedPeriods));
            if (daysElapsed >= termDays) {
                paidAmount = totalPayment;
            }
            BigDecimal remainingBalance = totalPayment.subtract(paidAmount).max(BigDecimal.ZERO);

            BigDecimal principalPaid = termDays > 0
                    ? loanAmount.multiply(BigDecimal.valueOf(elapsedPeriods)).divide(BigDecimal.valueOf(termDays), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            double progressPercent = 0.0;
            if (totalPayment.compareTo(BigDecimal.ZERO) > 0) {
                progressPercent = paidAmount.multiply(BigDecimal.valueOf(100))
                        .divide(totalPayment, 2, RoundingMode.HALF_UP)
                        .doubleValue();
                progressPercent = Math.min(100.0, Math.max(0.0, progressPercent));
            }

            String status;
            if (remainingPeriods == 0) {
                status = "PAID_OFF";
            } else if (daysElapsed > termDays) {
                status = "OVERDUE";
            } else {
                status = "ACTIVE";
            }

            String appId = doc.getApplicationId() != null ? doc.getApplicationId() : ("ML-" + doc.getId().substring(0, 6));
            String typeTitle = "VANITY".equalsIgnoreCase(doc.getMicroloanType()) ? "Роскошный максимум" : "Базовый минимум";
            String scheduleId = doc.getMetadata() != null && doc.getMetadata().get("scheduleId") != null
                    ? String.valueOf(doc.getMetadata().get("scheduleId"))
                    : null;

            User borrower = doc.getUserId() != null ? userCache.get(doc.getUserId()) : null;
            String borrowerName = (borrower != null && borrower.getFirstName() != null)
                    ? (borrower.getFirstName() + " " + (borrower.getLastName() != null ? borrower.getLastName() : "")).trim()
                    : (doc.getUserEmail() != null ? doc.getUserEmail().split("@")[0] : "Borrower");

            result.add(new AdminLoanMonitoringDto(
                    appId,
                    doc.getUserId(),
                    doc.getUserEmail() != null ? doc.getUserEmail() : "unknown@user.com",
                    borrowerName,
                    "MICROLOAN",
                    typeTitle,
                    doc.getLoanPurpose() != null ? doc.getLoanPurpose() : "Срочные потребительские нужды",
                    loanAmount.setScale(2, RoundingMode.HALF_UP),
                    totalPayment.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(0.8),
                    termDays + " дн.",
                    termDays,
                    doc.getCurrency() != null ? doc.getCurrency().name() : "RUB",
                    startDate,
                    elapsedPeriods,
                    remainingPeriods,
                    paidAmount.setScale(2, RoundingMode.HALF_UP),
                    principalPaid.setScale(2, RoundingMode.HALF_UP),
                    remainingBalance.setScale(2, RoundingMode.HALF_UP),
                    progressPercent,
                    dailyPayment.setScale(2, RoundingMode.HALF_UP),
                    status,
                    today.plusDays(1).toString(),
                    dailyPayment.setScale(2, RoundingMode.HALF_UP),
                    scheduleId
            ));
        }

        // Sort descending by startDate so recent loans appear first
        result.sort(Comparator.comparing(AdminLoanMonitoringDto::startDate).reversed());
        return result;
    }
}
