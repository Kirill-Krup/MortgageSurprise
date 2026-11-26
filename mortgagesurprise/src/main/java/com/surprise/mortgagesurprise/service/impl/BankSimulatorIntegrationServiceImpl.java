package com.surprise.mortgagesurprise.service.impl;

import com.surprise.common.dto.simulator.BlacklistCheckRequest;
import com.surprise.common.dto.simulator.BlacklistCheckResponse;
import com.surprise.common.dto.simulator.CreateSubjectRequest;
import com.surprise.common.dto.simulator.DocumentType;
import com.surprise.common.dto.simulator.RegisterEventRequest;
import com.surprise.common.dto.simulator.TrustEventType;
import com.surprise.mortgagesurprise.client.BankSimulatorBlacklistClient;
import com.surprise.mortgagesurprise.client.BankSimulatorSubjectClient;
import com.surprise.mortgagesurprise.client.BankSimulatorTrustEventClient;
import com.surprise.mortgagesurprise.model.entity.UserPassportData;
import com.surprise.mortgagesurprise.repository.jpa.UserPassportDataRepository;
import com.surprise.mortgagesurprise.service.BankSimulatorIntegrationService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankSimulatorIntegrationServiceImpl implements BankSimulatorIntegrationService {

    private static final String EVENT_SOURCE = "mortgagesurprise";

    private final BankSimulatorBlacklistClient blacklistClient;
    private final BankSimulatorSubjectClient subjectClient;
    private final BankSimulatorTrustEventClient trustEventClient;
    private final UserPassportDataRepository passportDataRepository;

    @Override
    public Optional<BlacklistCheckResponse> checkBlacklist(UUID userId) {
        Optional<String> documentHashOpt = resolvePassportHash(userId);
        if (documentHashOpt.isEmpty()) {
            log.warn("Cannot check blacklist in banksimulator for user {}: passport data is not filled", userId);
            return Optional.empty();
        }

        try {
            BlacklistCheckRequest request = BlacklistCheckRequest.builder()
                    .documentType(DocumentType.PASSPORT)
                    .documentHash(documentHashOpt.get())
                    .build();
            return Optional.ofNullable(blacklistClient.check(request));
        } catch (Exception e) {
            log.error("Failed to check blacklist in banksimulator for user {}", userId, e);
            return Optional.empty();
        }
    }

    @Override
    public void ensureSubjectCreatedForFirstMicroloan(UUID userId) {
        Optional<String> documentHashOpt = resolvePassportHash(userId);
        if (documentHashOpt.isEmpty()) {
            log.warn("Cannot create subject in banksimulator for user {}: passport data is not filled", userId);
            return;
        }

        String documentHash = documentHashOpt.get();
        try {
            CreateSubjectRequest request = CreateSubjectRequest.builder()
                    .documentType(DocumentType.PASSPORT)
                    .documentHash(documentHash)
                    .build();
            subjectClient.createSubject(request);
            log.info("Created subject in banksimulator for user {} on first microloan", userId);
        } catch (FeignException.BadRequest e) {
            log.info("Subject already exists in banksimulator for user {}: {}", userId, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create subject in banksimulator for user {}", userId, e);
        }
    }

    @Override
    public void recordMicroloanRepayment(UUID userId, String applicationId, boolean overdue, long overdueDays) {
        Optional<String> documentHashOpt = resolvePassportHash(userId);
        if (documentHashOpt.isEmpty()) {
            log.warn("Cannot register trust event in banksimulator for user {}: passport data is not filled", userId);
            return;
        }

        TrustEventType eventType = resolveRepaymentEventType(overdue, overdueDays);
        String eventKey = "microloan-repay-" + applicationId + "-" + eventType.name();

        try {
            RegisterEventRequest request = RegisterEventRequest.builder()
                    .eventKey(eventKey)
                    .documentHash(documentHashOpt.get())
                    .eventType(eventType)
                    .source(EVENT_SOURCE)
                    .occurredAt(Instant.now())
                    .details(Map.of(
                            "applicationId", applicationId,
                            "overdue", overdue,
                            "overdueDays", overdueDays
                    ))
                    .build();

            var response = trustEventClient.registerEvent(request);
            log.info("Registered {} event in banksimulator for user {} (appId={}): delta={}, scoreAfter={}",
                    eventType, userId, applicationId, response.delta(), response.scoreAfter());
        } catch (Exception e) {
            log.error("Failed to register {} event in banksimulator for user {} (appId={})",
                    eventType, userId, applicationId, e);
        }
    }

    private TrustEventType resolveRepaymentEventType(boolean overdue, long overdueDays) {
        if (!overdue) {
            return TrustEventType.PAYMENT_ON_TIME;
        }
        if (overdueDays >= 90) {
            return TrustEventType.OVERDUE_90_DAYS;
        }
        if (overdueDays >= 60) {
            return TrustEventType.OVERDUE_60_DAYS;
        }
        if (overdueDays >= 30) {
            return TrustEventType.OVERDUE_30_DAYS;
        }
        return TrustEventType.PAYMENT_DELAY;
    }

    private Optional<String> resolvePassportHash(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return passportDataRepository.findByUserId(userId)
                .map(UserPassportData::getPassportHash)
                .filter(hash -> hash != null && !hash.isBlank());
    }
}
