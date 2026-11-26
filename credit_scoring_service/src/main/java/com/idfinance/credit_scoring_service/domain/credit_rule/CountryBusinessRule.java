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

    @Override
    public Float handle(LoanApplicationRequest loanApplicationRequest) {
        if (loanApplicationRequest == null || loanApplicationRequest.country() == null) {
            log.info("No country provided in application");
            return handleNext(loanApplicationRequest);
        }
        String country = loanApplicationRequest.country();
        Set<CountryRuleSet> countryRuleSets = countryRuleSetRuleProvider.getRules();
        for (var rule : countryRuleSets) {
            if (rule.apply(country)) {
                float score = rule.creditScore() != null ? rule.creditScore() : 0f;
                return score + handleNext(loanApplicationRequest);
            }
        }
        log.info("No country rules matched");

        return handleNext(loanApplicationRequest);
    }

    @Override
    public int getOrder() {
        return 3;
    }
}