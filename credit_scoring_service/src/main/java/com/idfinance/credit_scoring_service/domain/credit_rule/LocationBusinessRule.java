package com.idfinance.credit_scoring_service.domain.credit_rule;

import com.idfinance.credit_scoring_service.domain.rule_set.CityRuleSet;
import com.idfinance.credit_scoring_service.domain.rule_set.CountryRuleSet;
import com.idfinance.credit_scoring_service.dto.LoanApplicationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public final class LocationBusinessRule extends CreditBusinessRule {

    private final Set<CityRuleSet> cityRuleSet;
    private final Set<CountryRuleSet> countryRuleSets;

    @Override
    public Float handle(LoanApplicationRequest loanApplicationRequest) {
        String city = loanApplicationRequest.city();
        String country = loanApplicationRequest.country();
        for (var rule : countryRuleSets) {
            if (rule.apply(country)) {
                this.creditScore += rule.creditScore();
                return handleNext(loanApplicationRequest);
            }
        }
        log.info("No country rules matched");
        for (var rule : cityRuleSet) {
            if (rule.apply(city)) {
                this.creditScore += rule.creditScore();
                return handleNext(loanApplicationRequest);
            }
        }
        log.info("No city rules matched");
        return handleNext(loanApplicationRequest);
    }

    @Override
    public int getOrder() {
        return 3;
    }
}