package com.idfinance.credit_scoring_service.domain.credit_rule;

import com.idfinance.credit_scoring_service.domain.rule_set.AgeRuleSet;
import com.idfinance.credit_scoring_service.dto.LoanApplicationRequest;
import com.idfinance.credit_scoring_service.provider.RuleProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public final class AgeBusinessRule extends CreditBusinessRule {

    private final RuleProvider<AgeRuleSet> ageRuleSetRuleProvider;

    @Override
    public Float handle(LoanApplicationRequest application) {
        if (application == null || application.age() == null) {
            log.info("No age provided in application");
            return handleNext(application);
        }
        int age = application.age();
        Set<AgeRuleSet> ruleSet = ageRuleSetRuleProvider.getRules();
        for (var rule : ruleSet) {
            if (rule.apply(age)) {
                float score = rule.creditScore() != null ? rule.creditScore() : 0f;
                return score + handleNext(application);
            }
        }
        log.info("No age rules matched");
        return handleNext(application);
    }

    @Override
    public int getOrder() {
        return 1;
    }
}