package com.idfinance.credit_scoring_service.domain.credit_rule;

import com.idfinance.credit_scoring_service.domain.rule_set.GenderRuleSet;
import com.idfinance.credit_scoring_service.dto.LoanApplicationRequest;
import com.idfinance.credit_scoring_service.provider.RuleProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public final class GenderBusinessRule extends CreditBusinessRule {

    private final RuleProvider<GenderRuleSet> genderRuleSetRuleProvider;

    @Override
    public Float handle(LoanApplicationRequest loanApplicationRequest) {
        if (loanApplicationRequest == null || loanApplicationRequest.gender() == null) {
            log.info("No gender provided in application");
            return handleNext(loanApplicationRequest);
        }
        String gender = loanApplicationRequest.gender();
        Set<GenderRuleSet> ruleSet = genderRuleSetRuleProvider.getRules();
        for (var rule : ruleSet) {
            if (rule.apply(gender)) {
                float score = rule.creditScore() != null ? rule.creditScore() : 0f;
                return score + handleNext(loanApplicationRequest);
            }
        }
        log.info("No gender rules matched");
        return handleNext(loanApplicationRequest);
    }

    @Override
    public int getOrder() {
        return 2;
    }
}