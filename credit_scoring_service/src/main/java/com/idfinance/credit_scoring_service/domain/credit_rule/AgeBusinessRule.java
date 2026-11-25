package com.idfinance.credit_scoring_service.domain.credit_rule;

import com.idfinance.credit_scoring_service.adapter.AgeConfigAdapter;
import com.idfinance.credit_scoring_service.domain.rule_set.AgeRuleSet;
import com.idfinance.credit_scoring_service.dto.LoanApplicationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public final class AgeBusinessRule extends CreditBusinessRule {

    private final AgeConfigAdapter ageConfigAdapter;
    private Set<AgeRuleSet> ruleSet = new HashSet<>();

    @Override
    public Float handle(LoanApplicationRequest application) {
        int age = application.age();
        updateRuleSet();
        for (var rule : ruleSet) {
            if (rule.apply(age)) {
                this.creditScore += rule.creditScore();
                return handleNext(application);
            }
        }
        log.info("No age rules matched");
        return handleNext(application);
    }

    @Override
    public int getOrder() {
        return 1;
    }

    private void updateRuleSet() {
        this.ruleSet = ageConfigAdapter.findAll();
    }
}