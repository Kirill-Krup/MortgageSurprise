package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.audit.BankOfferSelectionRequest;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResponse;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import com.surprise.mortgagesurprise.repository.mongo.UserActionAuditMongoRepository;
import com.surprise.mortgagesurprise.service.UserAuditService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAuditServiceImpl implements UserAuditService {

    private static final Logger log = LoggerFactory.getLogger(UserAuditServiceImpl.class);

    private final UserActionAuditMongoRepository auditRepository;

    private boolean shouldAudit(UUID userId, boolean isAdmin) {
        // Audit only authorized users who are NOT administrators
        return userId != null && !isAdmin;
    }

    private String normalizePurposeToEnglish(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return "General personal expenses";
        }
        String trimmed = purpose.trim();
        return switch (trimmed) {
            case "Повседневные неотложные расходы и покупка продуктов", "Daily urgent expenses and grocery shopping" ->
                    "Daily urgent expenses and grocery shopping";
            case "Срочный ремонт бытовой техники или сантехники", "Urgent home appliances or plumbing repairs" ->
                    "Urgent home appliances or plumbing repairs";
            case "Оплата медицинских услуг, анализов и лекарств", "Medical services, healthcare and prescription medicines" ->
                    "Medical services, healthcare and prescription medicines";
            case "Оплата коммунальных платежей или аренды", "Utility bills or apartment rent payment" ->
                    "Utility bills or apartment rent payment";
            case "Внезапные семейные траты", "Unforeseen urgent family expenses" ->
                    "Unforeseen urgent family expenses";
            case "Покупка сезонной одежды первой необходимости", "Purchase of essential seasonal clothing" ->
                    "Purchase of essential seasonal clothing";
            case "Покупка нового iPhone / флагманского смартфона", "Purchase of latest iPhone / flagship smartphone" ->
                    "Purchase of latest iPhone / flagship smartphone";
            case "Тюнинг автомобиля, покупка дисков и аудиосистемы", "Car tuning, luxury alloy rims and custom car audio" ->
                    "Car tuning, luxury alloy rims and custom car audio";
            case "Брендовая статусная одежда, часы и украшения", "Designer luxury wardrobe, luxury watches and jewelry" ->
                    "Designer luxury wardrobe, luxury watches and jewelry";
            case "Походы в клубы, рестораны и вечеринки", "Nightclubs, gourmet restaurants and VIP parties" ->
                    "Nightclubs, gourmet restaurants and VIP parties";
            case "Элитный отдых, фотосессии и развлечения", "Luxury travel, entertainment and photo shoots" ->
                    "Luxury travel, entertainment and photo shoots";
            case "Своя причина", "Other", "Custom purpose" ->
                    "Custom purpose";
            default -> trimmed;
        };
    }

    @Override
    public void auditMortgageCalculation(UUID userId, String userEmail, boolean isAdmin,
                                         CalculationRequest request, CalculationResponse response) {
        if (!shouldAudit(userId, isAdmin)) {
            return;
        }

        try {
            String tariffPart = (request.bankName() != null || request.offerName() != null)
                    ? " under tariff '" + (request.bankName() != null ? request.bankName() + " — " : "") + request.offerName() + "'"
                    : "";

            String desc = String.format("Mortgage calculation: %s %s for %d months at %s%%%s",
                    response.loanAmount(), response.currency(), response.termMonths(), response.annualInterestRate(), tariffPart);

            Map<String, Object> meta = new HashMap<>();
            meta.put("paymentType", response.paymentType());
            meta.put("propertyPrice", response.propertyPrice());
            meta.put("downPayment", response.downPayment());
            if (request.bankOfferId() != null) {
                meta.put("bankOfferId", request.bankOfferId());
            }

            UserActionAuditDocument doc = new UserActionAuditDocument(
                    UUID.randomUUID().toString(),
                    userId,
                    userEmail,
                    UserAuditActionType.MORTGAGE_CALCULATION,
                    desc,
                    request.bankOfferId(),
                    request.bankName(),
                    request.offerName(),
                    response.loanAmount(),
                    response.propertyPrice(),
                    response.downPayment(),
                    response.termMonths(),
                    null,
                    response.annualInterestRate(),
                    response.monthlyPayment(),
                    response.totalPayment(),
                    response.totalInterest(),
                    null,
                    null,
                    null,
                    response.currency(),
                    meta,
                    Instant.now()
            );

            auditRepository.save(doc);
            log.info("Audited MORTGAGE_CALCULATION for user {}: {}", userEmail, desc);
        } catch (Exception e) {
            log.error("Failed to audit mortgage calculation for user {}", userId, e);
        }
    }

    @Override
    public void auditMicroloanCalculation(UUID userId, String userEmail, boolean isAdmin,
                                          MicroloanCalculationRequest request, MicroloanCalculationResponse response) {
        if (!shouldAudit(userId, isAdmin)) {
            return;
        }

        try {
            String typeLabel = "VANITY".equalsIgnoreCase(response.microloanType().name())
                    ? "Luxury Maximum"
                    : "Basic Minimum";

            String cleanPurpose = normalizePurposeToEnglish(response.purpose());

            String desc = String.format("Microloan calculation: amount %s %s for %d days (Tier: '%s', Purpose: '%s')",
                    response.loanAmount(), response.currency(), response.termDays(), typeLabel, cleanPurpose);

            Map<String, Object> meta = new HashMap<>();
            meta.put("baseDailyRate", response.baseDailyRate());
            meta.put("vanitySurchargeRate", response.vanitySurchargeRate());
            meta.put("vanitySurchargeAmount", response.vanitySurchargeAmount());
            meta.put("effectiveDailyRate", response.effectiveDailyRate());

            UserActionAuditDocument doc = new UserActionAuditDocument(
                    UUID.randomUUID().toString(),
                    userId,
                    userEmail,
                    UserAuditActionType.MICROLOAN_CALCULATION,
                    desc,
                    null,
                    null,
                    null,
                    response.loanAmount(),
                    null,
                    null,
                    null,
                    response.termDays(),
                    response.effectiveDailyRate(),
                    null,
                    response.totalRepayment(),
                    response.totalInterest(),
                    response.microloanType().name(),
                    cleanPurpose,
                    null,
                    response.currency(),
                    meta,
                    Instant.now()
            );

            auditRepository.save(doc);
            log.info("Audited MICROLOAN_CALCULATION for user {}: {}", userEmail, desc);
        } catch (Exception e) {
            log.error("Failed to audit microloan calculation for user {}", userId, e);
        }
    }

    @Override
    public void auditMicroloanApplication(UUID userId, String userEmail, boolean isAdmin,
                                          MicroloanApplicationRequest request) {
        if (!shouldAudit(userId, isAdmin)) {
            return;
        }

        try {
            String typeLabel = "VANITY".equalsIgnoreCase(request.microloanType())
                    ? "Luxury Maximum"
                    : "Basic Minimum";

            String cleanPurpose = normalizePurposeToEnglish(request.purpose());

            String desc = String.format("Online microloan application %s: amount %s %s for %d days (Tier: '%s', Purpose: '%s', Borrower: %s)",
                    request.applicationId() != null ? request.applicationId() : "",
                    request.amount(), request.currency() != null ? request.currency() : com.surprise.mortgagesurprise.model.enums.Currency.RUB,
                    request.termDays(), typeLabel, cleanPurpose,
                    request.applicantFullName() != null ? request.applicantFullName() : userEmail);

            Map<String, Object> meta = new HashMap<>();
            meta.put("applicationId", request.applicationId());
            meta.put("scheduleId", request.scheduleId());
            meta.put("status", "APPROVED");

            UserActionAuditDocument doc = new UserActionAuditDocument(
                    UUID.randomUUID().toString(),
                    userId,
                    userEmail,
                    UserAuditActionType.MICROLOAN_APPLICATION,
                    desc,
                    null,
                    null,
                    null,
                    request.amount(),
                    null,
                    null,
                    null,
                    request.termDays(),
                    null,
                    null,
                    request.totalRepayment(),
                    null,
                    request.microloanType(),
                    cleanPurpose,
                    request.applicationId(),
                    request.currency(),
                    meta,
                    Instant.now()
            );

            auditRepository.save(doc);
            log.info("Audited MICROLOAN_APPLICATION for user {}: {}", userEmail, desc);
        } catch (Exception e) {
            log.error("Failed to audit microloan application for user {}", userId, e);
        }
    }

    @Override
    public void auditBankOfferSelected(UUID userId, String userEmail, boolean isAdmin,
                                       BankOfferSelectionRequest request) {
        if (!shouldAudit(userId, isAdmin)) {
            return;
        }

        try {
            String desc = String.format("Bank offer selected #%d: '%s — %s' (Rate: %s%%, Min down payment: %s%%, Term up to: %d mo.)",
                    request.bankOfferId(), request.bankName(), request.offerName(),
                    request.annualInterestRate(), request.minDownPaymentPercent(), request.maxTermMonths());

            Map<String, Object> meta = new HashMap<>();
            meta.put("minDownPaymentPercent", request.minDownPaymentPercent());
            meta.put("maxTermMonths", request.maxTermMonths());

            UserActionAuditDocument doc = new UserActionAuditDocument(
                    UUID.randomUUID().toString(),
                    userId,
                    userEmail,
                    UserAuditActionType.BANK_OFFER_SELECTED,
                    desc,
                    request.bankOfferId(),
                    request.bankName(),
                    request.offerName(),
                    null,
                    null,
                    null,
                    request.maxTermMonths(),
                    null,
                    request.annualInterestRate(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    request.currency(),
                    meta,
                    Instant.now()
            );

            auditRepository.save(doc);
            log.info("Audited BANK_OFFER_SELECTED for user {}: {}", userEmail, desc);
        } catch (Exception e) {
            log.error("Failed to audit bank offer selection for user {}", userId, e);
        }
    }

    @Override
    public List<UserActionAuditDocument> getAllAudits() {
        try {
            List<UserActionAuditDocument> list = auditRepository.findAllByOrderByTimestampDesc();
            return list != null ? list : List.of();
        } catch (Exception e) {
            log.error("Failed to fetch all audits", e);
            return List.of();
        }
    }

    @Override
    public List<UserActionAuditDocument> getAuditsByAction(UserAuditActionType actionType) {
        if (actionType == null) {
            return getAllAudits();
        }
        try {
            UserAuditActionType canonical = actionType.canonical();
            List<UserActionAuditDocument> list = auditRepository.findByActionTypeOrderByTimestampDesc(canonical);
            if ((list == null || list.isEmpty()) && canonical != actionType) {
                list = auditRepository.findByActionTypeOrderByTimestampDesc(actionType);
            }
            return list != null ? list : List.of();
        } catch (Exception e) {
            log.error("Failed to fetch audits by action {}", actionType, e);
            return List.of();
        }
    }

    @Override
    public List<UserActionAuditDocument> getAuditsForUser(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        try {
            List<UserActionAuditDocument> list = auditRepository.findByUserIdOrderByTimestampDesc(userId);
            return list != null ? list : List.of();
        } catch (Exception e) {
            log.error("Failed to fetch audits for user {}", userId, e);
            return List.of();
        }
    }
}
