package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.audit.BankOfferSelectionRequest;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResponse;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAuditService {

    void auditMortgageCalculation(UUID userId, String userEmail, boolean isAdmin,
                                  CalculationRequest request, CalculationResponse response);

    void auditMicroloanCalculation(UUID userId, String userEmail, boolean isAdmin,
                                   MicroloanCalculationRequest request, MicroloanCalculationResponse response);

    void auditMicroloanApplication(UUID userId, String userEmail, boolean isAdmin,
                                   MicroloanApplicationRequest request);

    void auditBankOfferSelected(UUID userId, String userEmail, boolean isAdmin,
                                BankOfferSelectionRequest request);

    List<UserActionAuditDocument> getAllAudits();

    List<UserActionAuditDocument> getAuditsByAction(UserAuditActionType actionType);

    List<UserActionAuditDocument> getAuditsForUser(UUID userId);

    boolean hasMicroloanApplications(UUID userId);

    Optional<UserActionAuditDocument> findMicroloanApplication(UUID userId, String applicationId);

    UserActionAuditDocument saveAudit(UserActionAuditDocument doc);
}
