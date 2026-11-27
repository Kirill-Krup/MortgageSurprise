package com.idfinance.credit_scoring_service.service.impl;

import com.idfinance.credit_scoring_service.domain.credit_rule.CreditRuleChain;
import com.idfinance.credit_scoring_service.dto.CreditDecisionResponse;
import com.idfinance.credit_scoring_service.dto.LoanApplicationRequest;
import com.idfinance.credit_scoring_service.service.CreditScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreditScoringServiceImpl implements CreditScoringService {

    private final CreditRuleChain creditRuleChain;

    @Override
    public CreditDecisionResponse getScoring(LoanApplicationRequest loanApplicationRequest) {
        return creditRuleChain.executeChain(loanApplicationRequest);
    }
}