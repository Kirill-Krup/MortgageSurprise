package com.idfinance.credit_scoring_service.service;

import com.idfinance.credit_scoring_service.dto.CreditDecisionResponse;
import com.idfinance.credit_scoring_service.dto.LoanApplicationRequest;

public interface CreditScoringService {
    CreditDecisionResponse getScoring(LoanApplicationRequest loanApplicationRequest);
}
