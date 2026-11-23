package com.idfinance.credit_scoring_service.domain;

import com.idfinance.credit_scoring_service.dto.LoanApplication;

public abstract class CreditBusinessRule implements HandlerLink<Float, LoanApplication, CreditBusinessRule> {

    protected CreditBusinessRule next;
    protected Float creditScore;

    @Override
    public final CreditBusinessRule link(CreditBusinessRule first, CreditBusinessRule... chain) {
        CreditBusinessRule head = first;
        for (var nextInChain : chain) {
            head.next = nextInChain;
            head = nextInChain;
        }
        return first;
    }

    @Override
    public final Float handleNext(LoanApplication loanApplication) {
        if (next == null) {
            return creditScore;
        }
        next.setCreditScore(creditScore);
        return next.handle(loanApplication);
    }

    private void setCreditScore(Float creditScore) {
        this.creditScore = creditScore;
    }
}