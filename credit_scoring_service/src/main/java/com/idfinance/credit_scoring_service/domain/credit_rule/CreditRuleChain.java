package com.idfinance.credit_scoring_service.domain.credit_rule;

import com.idfinance.credit_scoring_service.domain.HandlerChain;
import com.idfinance.credit_scoring_service.dto.CreditDecisionResponse;
import com.idfinance.credit_scoring_service.dto.LoanApplicationRequest;
import com.idfinance.credit_scoring_service.exception.ChainInterruptedException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class CreditRuleChain implements HandlerChain<CreditDecisionResponse, LoanApplicationRequest> {

    private final CreditBusinessRule ruleChain;

    @Override
    public CreditDecisionResponse executeChain(LoanApplicationRequest application) {
        float resultScore;
        try {
            resultScore = ruleChain.handle(application);
        } catch (ChainInterruptedException e) {
            return CreditDecisionResponse.rejected(e.getMessage());
        }
        return isPassed(resultScore) ? CreditDecisionResponse.approved(resultScore)
                : CreditDecisionResponse.rejected("Your credit score is too low");
    }

    private boolean isPassed(float resultScore) {
        return resultScore >= 0.5f;
    }
}