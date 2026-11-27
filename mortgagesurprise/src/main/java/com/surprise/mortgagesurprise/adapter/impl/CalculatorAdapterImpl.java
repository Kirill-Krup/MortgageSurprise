package com.surprise.mortgagesurprise.adapter.impl;

import com.surprise.mortgagesurprise.adapter.CalculatorAdapter;
import com.surprise.mortgagesurprise.dto.ErrorCode;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationRequest;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationResponse;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResponse;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.CalculatorService;
import com.surprise.mortgagesurprise.service.MicroloanApplicationService;
import com.surprise.mortgagesurprise.service.MicroloanService;
import com.surprise.mortgagesurprise.service.UserAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import static com.surprise.mortgagesurprise.util.CredentialValidator.isAdmin;

@Service
@RequiredArgsConstructor
public class CalculatorAdapterImpl implements CalculatorAdapter {

    private final CalculatorService calculatorService;
    private final MicroloanService microloanService;
    private final MicroloanApplicationService microloanApplicationService;
    private final UserAuditService userAuditService;

    @Override
    public CalculationResponse calculate(CalculationRequest request, CustomUserDetails userDetails) {
        ensureNotAdmin(userDetails);
        var userId = userDetails != null ? userDetails.getId() : null;
        var response = calculatorService.calculateAndPersist(request, userId);
        if (userDetails != null && Boolean.TRUE.equals(request.audit())) {
            userAuditService.auditMortgageCalculation(userId, userDetails.getUsername(), false, request, response);
        }
        return response;
    }

    @Override
    public MicroloanCalculationResponse calculateMicroloan(MicroloanCalculationRequest request, CustomUserDetails userDetails) {
        ensureNotAdmin(userDetails);
        var userId = userDetails != null ? userDetails.getId() : null;
        var response = microloanService.calculateAndPersist(request, userId);
        if (userDetails != null) {
            userAuditService.auditMicroloanCalculation(userId, userDetails.getUsername(), false, request, response);
        }
        return response;
    }

    @Override
    public MicroloanApplicationResponse applyMicroloan(MicroloanApplicationRequest request, CustomUserDetails userDetails) {
        ensureNotAdmin(userDetails);
        ensureAuthenticated(userDetails, "Authentication is required to apply for a microloan");
        return microloanApplicationService.apply(request, userDetails);
    }

    @Override
    public MicroloanApplicationResponse repayMicroloan(String applicationId, CustomUserDetails userDetails) {
        ensureNotAdmin(userDetails);
        ensureAuthenticated(userDetails, "Authentication is required to repay a microloan");
        return microloanApplicationService.repay(applicationId, userDetails);
    }

    private void ensureNotAdmin(CustomUserDetails userDetails) {
        if (isAdmin(userDetails)) {
            throw new AccessDeniedException(ErrorCode.ADMIN_CONFIGURER_ONLY.message);
        }
    }

    private void ensureAuthenticated(CustomUserDetails userDetails, String message) {
        if (userDetails == null) {
            throw new AccessDeniedException(message);
        }
    }
}
