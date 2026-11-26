package com.surprise.mortgagesurprise.service.impl;

import com.surprise.common.dto.scoring.CreditDecisionResponse;
import com.surprise.common.dto.scoring.LoanApplicationRequest;
import com.surprise.mortgagesurprise.client.CreditScoringClient;
import com.surprise.mortgagesurprise.model.entity.UserPassportData;
import com.surprise.mortgagesurprise.repository.jpa.UserPassportDataRepository;
import com.surprise.mortgagesurprise.security.jwt.JwtTokenProvider;
import com.surprise.mortgagesurprise.service.CreditScoringIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditScoringIntegrationServiceImpl implements CreditScoringIntegrationService {

    private final CreditScoringClient creditScoringClient;
    private final UserPassportDataRepository passportDataRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public CreditDecisionResponse evaluateScoring(UUID userId, String email, String role) {
        UserPassportData passportData = userId != null
                ? passportDataRepository.findByUserId(userId).orElse(null)
                : null;

        LoanApplicationRequest request = passportData != null
                ? new LoanApplicationRequest(
                        passportData.getAge(),
                        passportData.getCountry(),
                        passportData.getCity(),
                        passportData.getGender())
                : new LoanApplicationRequest(null, null, null, null);

        try {
            String token = jwtTokenProvider.generateAccessToken(userId, email, role);
            return creditScoringClient.getCreditScore("Bearer " + token, request);
        } catch (Exception e) {
            log.error("Failed to evaluate credit scoring for user {}", userId, e);
            return new CreditDecisionResponse(false, "Credit scoring service is unavailable", 0f);
        }
    }
}
