package com.surprise.mortgagesurprise.adapter.impl;

import com.surprise.mortgagesurprise.adapter.CalculatorServiceAdapter;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationRequest;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationResponse;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResponse;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.CalculatorService;
import com.surprise.mortgagesurprise.service.MicroloanService;
import com.surprise.mortgagesurprise.service.UserAuditService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import static com.surprise.mortgagesurprise.util.RoleValidator.isAdmin;

@Service
public class CalculatorServiceAdapterImpl implements CalculatorServiceAdapter {

    private final CalculatorService calculatorService;
    private final MicroloanService microloanService;
    private final UserAuditService userAuditService;

    public CalculatorServiceAdapterImpl(CalculatorService calculatorService, MicroloanService microloanService, UserAuditService userAuditService) {
        this.calculatorService = calculatorService;
        this.microloanService = microloanService;
        this.userAuditService = userAuditService;
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
        if (userDetails != null && !isAdmin) {
            userAuditService.auditMortgageCalculation(userId, userEmail, false, request, response);
        }
        return response;
    }

    @Override
    public MicroloanCalculationResponse calculateMicroloan(MicroloanCalculationRequest request, CustomUserDetails userDetails) {
        if (isAdmin(userDetails)) {
            throw new AccessDeniedException(
                    "Administrator acts solely as system configurator and cannot calculate or apply for loans"
            );
        }
        var userId = userDetails != null ? userDetails.getId() : null;
        return microloanService.calculateAndPersist(request, userId);
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
        userAuditService.auditMicroloanApplication(userDetails.getId(), userDetails.getUsername(), false, finalRequest);
        return new MicroloanApplicationResponse(true, appId, "APPROVED",
                "Microloan application registered successfully and recorded in bank audit system");
    }

    private boolean isApplicationIdExists(MicroloanApplicationRequest request) {
        return request.applicationId() != null && !request.applicationId().isBlank();
    }
}