package com.surprise.mortgagesurprise.adapter.impl;

import com.surprise.mortgagesurprise.adapter.CalculatorAdapter;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationRequest;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationResponse;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResponse;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.BankSimulatorIntegrationService;
import com.surprise.mortgagesurprise.service.CalculatorService;
import com.surprise.mortgagesurprise.service.MicroloanService;
import com.surprise.mortgagesurprise.service.UserAuditService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static com.surprise.mortgagesurprise.util.CredentialValidator.isAdmin;

@Service
public class CalculatorAdapterImpl implements CalculatorAdapter {

    private final CalculatorService calculatorService;
    private final MicroloanService microloanService;
    private final UserAuditService userAuditService;
    private final BankSimulatorIntegrationService bankSimulatorIntegrationService;

    public CalculatorAdapterImpl(CalculatorService calculatorService,
                                 MicroloanService microloanService,
                                 UserAuditService userAuditService,
                                 BankSimulatorIntegrationService bankSimulatorIntegrationService) {
        this.calculatorService = calculatorService;
        this.microloanService = microloanService;
        this.userAuditService = userAuditService;
        this.bankSimulatorIntegrationService = bankSimulatorIntegrationService;
    }

    @Override
    public CalculationResponse calculate(CalculationRequest request, CustomUserDetails userDetails) {
        if (isAdmin(userDetails)) {
            throw new AccessDeniedException(
                    "Administrator acts solely as system configurator and cannot calculate or apply for loans"
            );
        }
        var userId = userDetails != null ? userDetails.getId() : null;
        var userEmail = userDetails != null ? userDetails.getUsername() : null;
        var isAdmin = isAdmin(userDetails);
        var response = calculatorService.calculateAndPersist(request, userId);
        if (userDetails != null && !isAdmin && Boolean.TRUE.equals(request.audit())) {
            userAuditService.auditMortgageCalculation(userId, userEmail, false, request, response);
        }
        return response;
    }

    @Override
    public MicroloanCalculationResponse calculateMicroloan(MicroloanCalculationRequest request, CustomUserDetails userDetails) {
        if (isAdmin(userDetails)) {
            throw new AccessDeniedException("Administrator acts solely as system configurator and" +
                    " cannot calculate or apply for loans");
        }
        var userId = userDetails != null ? userDetails.getId() : null;
        var userEmail = userDetails != null ? userDetails.getUsername() : null;
        var isAdmin = isAdmin(userDetails);
        var response = microloanService.calculateAndPersist(request, userId);
        if (userDetails != null && !isAdmin) {
            userAuditService.auditMicroloanCalculation(userId, userEmail, false, request, response);
        }
        return response;
    }

    @Override
    public MicroloanApplicationResponse applyMicroloan(MicroloanApplicationRequest request, CustomUserDetails userDetails) {
        if (isAdmin(userDetails)) {
            throw new AccessDeniedException("Administrator acts solely as system configurator " +
                    "and cannot calculate or apply for loans");
        }
        if (userDetails == null) {
            throw new AccessDeniedException("Authentication is required to apply for a microloan");
        }
        var appId = isApplicationIdExists(request)
                ? request.applicationId()
                : "MS-" + (int) (10000 + Math.random() * 90000);
        var finalRequest = new MicroloanApplicationRequest(
                request.amount(),
                request.termDays(),
                request.purpose(),
                request.microloanType(),
                request.totalRepayment(),
                request.scheduleId(),
                appId,
                request.applicantFullName() != null ? request.applicantFullName() : userDetails.getUsername(),
                request.currency()
        );
        boolean isFirstMicroloan = !userAuditService.hasMicroloanApplications(userDetails.getId());
        userAuditService.auditMicroloanApplication(userDetails.getId(), userDetails.getUsername(), false, finalRequest);
        if (isFirstMicroloan) {
            bankSimulatorIntegrationService.ensureSubjectCreatedForFirstMicroloan(userDetails.getId());
        }
        return new MicroloanApplicationResponse(true, appId, "APPROVED",
                "Microloan application registered successfully and recorded in bank audit system");
    }

    @Override
    public MicroloanApplicationResponse repayMicroloan(String applicationId, CustomUserDetails userDetails) {
        if (isAdmin(userDetails)) {
            throw new AccessDeniedException("Administrator acts solely as system configurator " +
                    "and cannot repay loans");
        }
        if (userDetails == null) {
            throw new AccessDeniedException("Authentication is required to repay a microloan");
        }

        var doc = userAuditService.findMicroloanApplication(userDetails.getId(), applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Microloan application not found: " + applicationId));

        Map<String, Object> metadata = doc.getMetadata() != null
                ? new HashMap<>(doc.getMetadata())
                : new HashMap<>();

        if ("PAID_OFF".equalsIgnoreCase(String.valueOf(metadata.get("status")))) {
            return new MicroloanApplicationResponse(true, applicationId, "PAID_OFF",
                    "Microloan is already repaid");
        }

        Instant start = doc.getTimestamp() != null ? doc.getTimestamp() : Instant.now();
        int termDays = doc.getTermDays() != null ? doc.getTermDays() : 14;
        long daysElapsed = Math.max(0, ChronoUnit.DAYS.between(start, Instant.now()));
        boolean overdue = daysElapsed > termDays;
        long overdueDays = overdue ? (daysElapsed - termDays) : 0;

        metadata.put("status", "PAID_OFF");
        metadata.put("repaidAt", Instant.now().toString());
        metadata.put("overdue", overdue);
        metadata.put("overdueDays", overdueDays);
        doc.setMetadata(metadata);
        userAuditService.saveAudit(doc);

        bankSimulatorIntegrationService.recordMicroloanRepayment(userDetails.getId(), applicationId, overdue, overdueDays);

        String status = overdue ? "PAID_OFF_OVERDUE" : "PAID_OFF";
        String message = overdue
                ? "Microloan repaid with overdue (" + overdueDays + " days); trust score decreased"
                : "Microloan repaid on time; trust score increased";
        return new MicroloanApplicationResponse(true, applicationId, status, message);
    }

    private boolean isApplicationIdExists(MicroloanApplicationRequest request) {
        return request.applicationId() != null && !request.applicationId().isBlank();
    }
}