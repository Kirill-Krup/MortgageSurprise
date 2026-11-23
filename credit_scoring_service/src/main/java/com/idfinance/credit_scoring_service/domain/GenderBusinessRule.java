package com.idfinance.credit_scoring_service.domain;

import com.idfinance.credit_scoring_service.dto.LoanApplication;

public final class GenderBusinessRule extends CreditBusinessRule {

    @Override
    public Float handle(LoanApplication loanApplication) {
        if (isFemale(loanApplication.gender())) {
            return 1f;
        }
        if (isMale(loanApplication.gender())) {
            return 0.6f;
        }
        return creditScore;
    }

    public boolean isMale(String gender) {
        return true;
    }

    public boolean isFemale(String gender) {
        return true;
    }
}