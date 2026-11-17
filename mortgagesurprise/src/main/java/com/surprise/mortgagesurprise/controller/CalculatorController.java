package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationRequest;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationResponse;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResponse;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.CalculatorService;
import com.surprise.mortgagesurprise.service.MicroloanService;
import com.surprise.mortgagesurprise.service.UserAuditService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calculator")
public class CalculatorController {

    private final CalculatorService calculatorService;
    private final MicroloanService microloanService;
    private final UserAuditService userAuditService;

    public CalculatorController(CalculatorService calculatorService,
                                MicroloanService microloanService,
                                UserAuditService userAuditService) {
        this.calculatorService = calculatorService;
        this.microloanService = microloanService;
        this.userAuditService = userAuditService;
    }

    @PostMapping({"/calculate", "/mortgage"})
    public ResponseEntity<CalculationResponse> calculate(
            @Valid @RequestBody CalculationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        checkNotAdmin(userDetails);
        UUID userId = userDetails != null ? userDetails.getId() : null;
        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        boolean isAdmin = isAdmin(userDetails);

        CalculationResponse response = calculatorService.calculateAndPersist(request, userId);

        if (userDetails != null && !isAdmin) {
            userAuditService.auditMortgageCalculation(userId, userEmail, false, request, response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/microloan")
    public ResponseEntity<MicroloanCalculationResponse> calculateMicroloan(
            @Valid @RequestBody MicroloanCalculationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        checkNotAdmin(userDetails);
        UUID userId = userDetails != null ? userDetails.getId() : null;

        MicroloanCalculationResponse response = microloanService.calculateAndPersist(request, userId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/microloan/apply")
    public ResponseEntity<MicroloanApplicationResponse> applyMicroloan(
            @Valid @RequestBody MicroloanApplicationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        checkNotAdmin(userDetails);
        if (userDetails == null) {
            throw new AccessDeniedException(
                    "Authentication is required to apply for a microloan"
            );
        }

        String appId = request.applicationId() != null && !request.applicationId().isBlank()
                ? request.applicationId()
                : "MS-" + (int)(10000 + Math.random() * 90000);

        MicroloanApplicationRequest finalRequest = new MicroloanApplicationRequest(
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

        return ResponseEntity.ok(new MicroloanApplicationResponse(
                true,
                appId,
                "APPROVED",
                "Microloan application registered successfully and recorded in bank audit system"
        ));
    }

    private boolean isAdmin(CustomUserDetails userDetails) {
        return userDetails != null && userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void checkNotAdmin(CustomUserDetails userDetails) {
        if (isAdmin(userDetails)) {
            throw new AccessDeniedException(
                    "Administrator acts solely as system configurator and cannot calculate or apply for loans"
            );
        }
    }

    @GetMapping("/schedules/{scheduleId}")
    public ResponseEntity<PaymentScheduleDocument> getSchedule(@PathVariable String scheduleId) {
        PaymentScheduleDocument schedule = calculatorService.getScheduleById(scheduleId);
        return ResponseEntity.ok(schedule);
    }
}
