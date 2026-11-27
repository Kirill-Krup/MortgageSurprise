package com.surprise.mortgagesurprise.client;

import com.surprise.common.dto.scoring.CreditDecisionResponse;
import com.surprise.common.dto.scoring.LoanApplicationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign-клиент для CreditScoringController (/api/v1/credit-score) сервиса credit_scoring_service.
 */
@FeignClient(
        name = "credit-scoring-service",
        url = "${credit.scoring.url}"
)
public interface CreditScoringClient {

    @PostMapping("/credit-score")
    CreditDecisionResponse getCreditScore(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody LoanApplicationRequest request
    );
}
