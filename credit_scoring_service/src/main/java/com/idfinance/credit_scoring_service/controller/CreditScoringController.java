package com.idfinance.credit_scoring_service.controller;

import com.idfinance.credit_scoring_service.dto.CreditDecisionResponse;
import com.idfinance.credit_scoring_service.dto.LoanApplicationRequest;
import com.idfinance.credit_scoring_service.service.CreditScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/credit-score")
public class CreditScoringController {

    private final CreditScoringService creditScoringService;

    @PostMapping
    public ResponseEntity<CreditDecisionResponse> getCreditScoringResponse(
            @RequestBody LoanApplicationRequest loanApplicationRequest) {
        return ResponseEntity.ok(creditScoringService.getScoring(loanApplicationRequest));
    }
}