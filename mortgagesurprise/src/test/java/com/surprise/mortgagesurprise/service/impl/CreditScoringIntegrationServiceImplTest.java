package com.surprise.mortgagesurprise.service.impl;

import com.surprise.common.dto.scoring.CreditDecisionResponse;
import com.surprise.common.dto.scoring.LoanApplicationRequest;
import com.surprise.mortgagesurprise.client.CreditScoringClient;
import com.surprise.mortgagesurprise.model.entity.UserPassportData;
import com.surprise.mortgagesurprise.repository.jpa.UserPassportDataRepository;
import com.surprise.mortgagesurprise.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditScoringIntegrationServiceImplTest {

    @Mock
    private CreditScoringClient creditScoringClient;

    @Mock
    private UserPassportDataRepository passportDataRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private CreditScoringIntegrationServiceImpl service;

    @Test
    void evaluateScoring_sendsPassportDemographicsAndBearerToken() {
        UUID userId = UUID.randomUUID();
        UserPassportData passport = new UserPassportData();
        passport.setUserId(userId);
        passport.setAge(29);
        passport.setCountry("RU");
        passport.setCity("Moscow");
        passport.setGender("MALE");

        when(passportDataRepository.findByUserId(userId)).thenReturn(Optional.of(passport));
        when(jwtTokenProvider.generateAccessToken(userId, "user@example.com", "ROLE_USER")).thenReturn("jwt-123");
        when(creditScoringClient.getCreditScore("Bearer jwt-123", new LoanApplicationRequest(29, "RU", "Moscow", "MALE")))
                .thenReturn(new CreditDecisionResponse(true, null, 0.85f));

        CreditDecisionResponse response = service.evaluateScoring(userId, "user@example.com", "ROLE_USER");

        assertThat(response.approved()).isTrue();
        assertThat(response.score()).isEqualTo(0.85f);
        verify(creditScoringClient).getCreditScore("Bearer jwt-123", new LoanApplicationRequest(29, "RU", "Moscow", "MALE"));
    }

    @Test
    void evaluateScoring_returnsRejectedWhenClientFails() {
        UUID userId = UUID.randomUUID();
        when(passportDataRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(jwtTokenProvider.generateAccessToken(userId, "user@example.com", "ROLE_USER")).thenReturn("jwt-123");
        when(creditScoringClient.getCreditScore("Bearer jwt-123", new LoanApplicationRequest(null, null, null, null)))
                .thenThrow(new RuntimeException("Connection refused"));

        CreditDecisionResponse response = service.evaluateScoring(userId, "user@example.com", "ROLE_USER");

        assertThat(response.approved()).isFalse();
        assertThat(response.reason()).isEqualTo("Credit scoring service is unavailable");
    }
}
