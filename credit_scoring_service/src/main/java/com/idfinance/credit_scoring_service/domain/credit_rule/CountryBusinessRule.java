package com.idfinance.credit_scoring_service.domain.credit_rule;

import com.idfinance.credit_scoring_service.domain.rule_set.CountryRuleSet;
import com.idfinance.credit_scoring_service.dto.LoanApplicationRequest;
import com.idfinance.credit_scoring_service.provider.RuleProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public final class CountryBusinessRule extends CreditBusinessRule {

    private final RuleProvider<CountryRuleSet> countryRuleSetRuleProvider;
    private Set<CountryRuleSet> countryRuleSets = new HashSet<>();

    @Override
    public Float handle(LoanApplicationRequest loanApplicationRequest) {
        String country = loanApplicationRequest.country();
        updateCountryRules();
        for (var rule : countryRuleSets) {
            if (rule.apply(country)) {
                this.creditScore += rule.creditScore();
                return handleNext(loanApplicationRequest);
            }
        }
        log.info("No country rules matched");

        return handleNext(loanApplicationRequest);
    }

    @Override
    public int getOrder() {
        return 3;
    }

    private void updateCountryRules() {
        this.countryRuleSets = countryRuleSetRuleProvider.getRules();
    }
}