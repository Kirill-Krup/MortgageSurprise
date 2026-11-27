package com.idfinance.credit_scoring_service.domain.credit_rule;

import com.idfinance.credit_scoring_service.exception.NonUniqueOrderException;

import java.util.SortedMap;
import java.util.TreeMap;

public final class CreditChainBuilder {

    private final SortedMap<Integer, CreditBusinessRule> businessRuleSortedMap = new TreeMap<>();

    public static CreditChainBuilder builder() {
        return new CreditChainBuilder();
    }

    public CreditChainBuilder addBusinessRule(CreditBusinessRule rule) {
        if (businessRuleSortedMap.containsKey(rule.getOrder())) {
            throw new NonUniqueOrderException("Business rule with this order already exists");
        }
        businessRuleSortedMap.put(rule.getOrder(), rule);
        return this;
    }

    public CreditRuleChain build() {
        var firstRule = businessRuleSortedMap.firstEntry().getValue();
        firstRule.link(firstRule, businessRuleSortedMap.values()
                .toArray(CreditBusinessRule[]::new));
        return new CreditRuleChain(firstRule);
    }

    private CreditChainBuilder() {
    }
}