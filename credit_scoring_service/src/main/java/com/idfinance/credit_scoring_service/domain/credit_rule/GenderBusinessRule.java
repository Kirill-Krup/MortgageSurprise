package com.idfinance.credit_scoring_service.domain.credit_rule;

import com.idfinance.credit_scoring_service.adapter.GenderConfigAdapter;
import com.idfinance.credit_scoring_service.domain.rule_set.GenderRuleSet;
import com.idfinance.credit_scoring_service.dto.LoanApplicationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public final class GenderBusinessRule extends CreditBusinessRule {

    private final GenderConfigAdapter genderConfigAdapter;
    private Set<GenderRuleSet> ruleSet = new HashSet<>();

    @Override
    public Float handle(LoanApplicationRequest loanApplicationRequest) {
        String gender = loanApplicationRequest.gender();
        updateRuleSet();
        for (var rule : ruleSet) {
            if (rule.apply(gender)) {
                this.creditScore += rule.creditScore();
                return handleNext(loanApplicationRequest);
            }
        }
        log.info("No gender rules matched");
        return handleNext(loanApplicationRequest);
    }

    @Override
    public int getOrder() {
        return 2;
    }

    private void updateRuleSet() {
        this.ruleSet = genderConfigAdapter.findAll();
    }
}