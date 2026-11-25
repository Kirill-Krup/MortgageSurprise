package com.idfinance.credit_scoring_service.domain.credit_rule;

import com.idfinance.credit_scoring_service.domain.rule_set.CityRuleSet;
import com.idfinance.credit_scoring_service.dto.LoanApplicationRequest;
import com.idfinance.credit_scoring_service.provider.RuleProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class CityBusinessRule extends CreditBusinessRule {

    private final RuleProvider<CityRuleSet> cityRuleSetRuleProvider;
    private Set<CityRuleSet> cityRuleSet = new HashSet<>();

    @Override
    public Float handle(LoanApplicationRequest loanApplicationRequest) {
        String city = loanApplicationRequest.city();
        updateCityRules();
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
        return 4;
    }

    private void updateCityRules() {
        this.cityRuleSet = cityRuleSetRuleProvider.getRules();
    }
}