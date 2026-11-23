package com.idfinance.credit_scoring_service.domain;

import com.idfinance.credit_scoring_service.dto.LoanApplication;

public final class LocationBusinessRule extends CreditBusinessRule {

    @Override
    public Float handle(LoanApplication loanApplication) {
        if (isFromAnotherCountry(loanApplication.country())) {
            return 0.5f;
        }
        if (isFromPreferredCities(loanApplication.city())) {
            return 0.9f;
        }
        if (!isFromPreferredCities(loanApplication.city())) {
            return 0.2f;
        }
        return creditScore;
    }

    public boolean isFromPreferredCities(String city) {
        return true;
    }

    public boolean isFromAnotherCountry(String country) {
        return true;
    }
}
