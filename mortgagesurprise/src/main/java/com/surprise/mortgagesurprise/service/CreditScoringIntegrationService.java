package com.surprise.mortgagesurprise.service;

import com.surprise.common.dto.scoring.CreditDecisionResponse;

import java.util.UUID;

public interface CreditScoringIntegrationService {

    CreditDecisionResponse evaluateScoring(UUID userId, String email, String role);
}
