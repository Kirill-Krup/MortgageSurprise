package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.calculator.CalculationCacheKeyGenerator;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.service.CalculationCacheService;
import com.surprise.mortgagesurprise.service.CalculationHistoryService;
import com.surprise.mortgagesurprise.service.CalculatorService;
import com.surprise.mortgagesurprise.service.MortgageCalculator;
import com.surprise.mortgagesurprise.service.PaymentScheduleService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Single responsibility: Orchestrate the calculation use-case (caching, calculation, schedule & history persistence).
 */
@Service
public class CalculatorServiceImpl implements CalculatorService {

    private final MortgageCalculator mortgageCalculator;
    private final PaymentScheduleService paymentScheduleService;
    private final CalculationHistoryService historyService;
    private final CalculationCacheService cacheService;
    private final CalculationCacheKeyGenerator cacheKeyGenerator;

    public CalculatorServiceImpl(MortgageCalculator mortgageCalculator,
                                 PaymentScheduleService paymentScheduleService,
                                 CalculationHistoryService historyService,
                                 CalculationCacheService cacheService,
                                 CalculationCacheKeyGenerator cacheKeyGenerator) {
        this.mortgageCalculator = mortgageCalculator;
        this.paymentScheduleService = paymentScheduleService;
        this.historyService = historyService;
        this.cacheService = cacheService;
        this.cacheKeyGenerator = cacheKeyGenerator;
    }

    @Override
    public CalculationResponse calculateAndPersist(CalculationRequest request, UUID userId) {
        String hash = cacheKeyGenerator.generateKey(request);

        Optional<CalculationResponse> cachedResponse = cacheService.getFromCache(hash);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }

        CalculationResult result = mortgageCalculator.calculate(request);

        PaymentScheduleDocument savedSchedule = paymentScheduleService.saveSchedule(result, hash);
        historyService.recordHistory(userId, savedSchedule.getId(), result);

        CalculationResponse response = toResponse(savedSchedule.getId(), result);
        cacheService.putInCache(hash, response);

        return response;
    }

    @Override
    public PaymentScheduleDocument getScheduleById(String scheduleId) {
        return paymentScheduleService.getScheduleById(scheduleId);
    }

    private CalculationResponse toResponse(String scheduleId, CalculationResult result) {
        return new CalculationResponse(
                scheduleId,
                result.propertyPrice(),
                result.downPayment(),
                result.loanAmount(),
                result.termMonths(),
                result.actualTermMonths(),
                result.annualInterestRate(),
                result.paymentType().name(),
                result.currency(),
                result.monthlyPayment(),
                result.minMonthlyPayment(),
                result.maxMonthlyPayment(),
                result.totalInterest(),
                result.totalPayment(),
                result.schedule()
        );
    }
}
