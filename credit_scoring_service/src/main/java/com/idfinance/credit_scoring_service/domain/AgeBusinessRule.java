package com.idfinance.credit_scoring_service.domain;

import com.idfinance.credit_scoring_service.dto.LoanApplication;

public final class AgeBusinessRule extends CreditBusinessRule {

    private static final int MIN_AGE = 18; // TODO change to configurable via API
    private static final int MAX_AGE = 90; // TODO change to configurable via API

    @Override
    public Float handle(LoanApplication application) {
        int age = application.age();
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new IllegalArgumentException("Age must be between " + MIN_AGE + " and " + MAX_AGE);
        }
        return handleNext(application);
    }
}