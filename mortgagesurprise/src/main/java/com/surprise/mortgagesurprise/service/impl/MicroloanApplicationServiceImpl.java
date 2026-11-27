package com.surprise.mortgagesurprise.service.impl;

import com.surprise.common.dto.scoring.CreditDecisionResponse;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationRequest;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationResponse;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.BankSimulatorIntegrationService;
import com.surprise.mortgagesurprise.service.CreditScoringIntegrationService;
import com.surprise.mortgagesurprise.service.MicroloanApplicationService;
import com.surprise.mortgagesurprise.service.UserAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MicroloanApplicationServiceImpl implements MicroloanApplicationService {

    private static final int DEFAULT_TERM_DAYS = 14;
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_PAID_OFF = "PAID_OFF";
    private static final String STATUS_PAID_OFF_OVERDUE = "PAID_OFF_OVERDUE";
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserAuditService userAuditService;
    private final BankSimulatorIntegrationService bankSimulatorIntegrationService;
    private final CreditScoringIntegrationService creditScoringIntegrationService;

    @Override
    public MicroloanApplicationResponse apply(MicroloanApplicationRequest request, CustomUserDetails userDetails) {
        String appId = resolveApplicationId(request);

        MicroloanApplicationResponse blacklistRejection = checkBlacklist(userDetails.getId(), appId);
        if (blacklistRejection != null) {
            return blacklistRejection;
        }

        MicroloanApplicationResponse scoringRejection = evaluateScoring(userDetails, appId);
        if (scoringRejection != null) {
            return scoringRejection;
        }

        MicroloanApplicationRequest enrichedRequest = enrichRequest(request, userDetails, appId);
        boolean isFirstMicroloan = !userAuditService.hasMicroloanApplications(userDetails.getId());
        userAuditService.auditMicroloanApplication(userDetails.getId(), userDetails.getUsername(), false, enrichedRequest);
        if (isFirstMicroloan) {
            bankSimulatorIntegrationService.ensureSubjectCreatedForFirstMicroloan(userDetails.getId());
        }

        return new MicroloanApplicationResponse(true, appId, STATUS_APPROVED,
                "Microloan application registered successfully and recorded in bank audit system");
    }

    @Override
    public MicroloanApplicationResponse repay(String applicationId, CustomUserDetails userDetails) {
        UserActionAuditDocument doc = userAuditService.findMicroloanApplication(userDetails.getId(), applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Microloan application not found: " + applicationId));

        Map<String, Object> metadata = doc.getMetadata() != null
                ? new HashMap<>(doc.getMetadata())
                : new HashMap<>();

        if (STATUS_PAID_OFF.equalsIgnoreCase(String.valueOf(metadata.get("status")))) {
            return new MicroloanApplicationResponse(true, applicationId, STATUS_PAID_OFF,
                    "Microloan is already repaid");
        }

        Instant start = doc.getTimestamp() != null ? doc.getTimestamp() : Instant.now();
        int termDays = doc.getTermDays() != null ? doc.getTermDays() : DEFAULT_TERM_DAYS;
        long daysElapsed = Math.max(0, ChronoUnit.DAYS.between(start, Instant.now()));
        boolean overdue = daysElapsed > termDays;
        long overdueDays = overdue ? (daysElapsed - termDays) : 0;

        metadata.put("status", STATUS_PAID_OFF);
        metadata.put("repaidAt", Instant.now().toString());
        metadata.put("overdue", overdue);
        metadata.put("overdueDays", overdueDays);
        doc.setMetadata(metadata);
        userAuditService.saveAudit(doc);

        bankSimulatorIntegrationService.recordMicroloanRepayment(userDetails.getId(), applicationId, overdue, overdueDays);

        String status = overdue ? STATUS_PAID_OFF_OVERDUE : STATUS_PAID_OFF;
        String message = overdue
                ? "Microloan repaid with overdue (" + overdueDays + " days); trust score decreased"
                : "Microloan repaid on time; trust score increased";
        return new MicroloanApplicationResponse(true, applicationId, status, message);
    }

    private String resolveApplicationId(MicroloanApplicationRequest request) {
        if (request.applicationId() != null && !request.applicationId().isBlank()) {
            return request.applicationId();
        }
        return "MS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private MicroloanApplicationResponse checkBlacklist(UUID userId, String appId) {
        var blacklistOpt = bankSimulatorIntegrationService.checkBlacklist(userId);
        if (blacklistOpt.isPresent() && (blacklistOpt.get().blacklisted() || blacklistOpt.get().hardBlock())) {
            var reasons = blacklistOpt.get().reasons();
            String reason = reasons != null && !reasons.isEmpty()
                    ? String.join("; ", reasons)
                    : "Applicant is blacklisted";
            return new MicroloanApplicationResponse(false, appId, STATUS_REJECTED, reason);
        }
        return null;
    }

    private MicroloanApplicationResponse evaluateScoring(CustomUserDetails userDetails, String appId) {
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse(DEFAULT_ROLE);
        CreditDecisionResponse decision = creditScoringIntegrationService.evaluateScoring(
                userDetails.getId(), userDetails.getUsername(), role);
        if (decision == null || !decision.approved()) {
            String reason = decision != null && decision.reason() != null
                    ? decision.reason()
                    : "Credit scoring rejected the application";
            return new MicroloanApplicationResponse(false, appId, STATUS_REJECTED, reason);
        }
        return null;
    }

    private MicroloanApplicationRequest enrichRequest(
            MicroloanApplicationRequest request,
            CustomUserDetails userDetails,
            String appId
    ) {
        String applicantName = request.applicantFullName() != null
                ? request.applicantFullName()
                : userDetails.getUsername();
        return new MicroloanApplicationRequest(
                request.amount(),
                request.termDays(),
                request.purpose(),
                request.microloanType(),
                request.totalRepayment(),
                request.scheduleId(),
                appId,
                applicantName,
                request.currency()
        );
    }
}
