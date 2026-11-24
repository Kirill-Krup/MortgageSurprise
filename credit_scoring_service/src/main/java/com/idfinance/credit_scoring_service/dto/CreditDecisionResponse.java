package com.idfinance.credit_scoring_service.dto;

public record CreditDecisionResponse(boolean approved, String reason, float score) {

    public static CreditDecisionResponse approved(float score) {
        return new CreditDecisionResponse(true, null, score);
    }

    public static CreditDecisionResponse rejected(String reason) {
        return new CreditDecisionResponse(false, reason, 0f);
    }
}