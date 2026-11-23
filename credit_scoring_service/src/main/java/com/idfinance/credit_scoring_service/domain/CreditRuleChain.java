package com.idfinance.credit_scoring_service.domain;

import com.idfinance.credit_scoring_service.dto.CreditDecision;
import com.idfinance.credit_scoring_service.dto.LoanApplication;
import com.idfinance.credit_scoring_service.exception.ChainInterruptedException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class CreditRuleChain implements HandlerChain<CreditDecision, LoanApplication> {

    private final CreditBusinessRule ruleChain;

    @Override
    public CreditDecision executeChain(LoanApplication application) {
        float resultScore;
        try {
            resultScore = ruleChain.handle(application);
        } catch (ChainInterruptedException e) {
            return CreditDecision.rejected(e.getMessage());
        }
        return isPassed(resultScore) ? CreditDecision.approved(resultScore)
                : CreditDecision.rejected("");
    }

    private boolean isPassed(float resultScore) {
        return resultScore >= 0.5f;
    }
}