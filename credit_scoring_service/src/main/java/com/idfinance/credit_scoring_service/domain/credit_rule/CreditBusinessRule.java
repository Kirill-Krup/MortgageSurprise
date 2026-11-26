package com.idfinance.credit_scoring_service.domain.credit_rule;

import com.idfinance.credit_scoring_service.domain.HandlerLink;
import com.idfinance.credit_scoring_service.domain.Ordered;
import com.idfinance.credit_scoring_service.dto.LoanApplicationRequest;

public abstract class CreditBusinessRule implements HandlerLink<Float, LoanApplicationRequest, CreditBusinessRule>,
        Ordered {

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
    public final Float handleNext(LoanApplicationRequest loanApplicationRequest) {
        if (next == null) {
            return creditScore;
        }
        next.setCreditScore(creditScore);
        return next.handle(loanApplicationRequest);
    }

    private void setCreditScore(Float creditScore) {
        this.creditScore = creditScore;
    }
}