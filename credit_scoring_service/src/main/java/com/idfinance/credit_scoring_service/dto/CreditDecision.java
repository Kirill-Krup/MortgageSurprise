package com.idfinance.credit_scoring_service.dto;

public record CreditDecision(boolean approved, String reason, float score) {

    public static CreditDecision approved(float score) {
        return new CreditDecision(true, null, score);
    }

    public static CreditDecision rejected(String reason) {
        return new CreditDecision(false, reason, 0f);
    }
}