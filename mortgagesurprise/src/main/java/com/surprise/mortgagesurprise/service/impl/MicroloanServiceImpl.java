package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.calculator.CalculationCacheKeyGenerator;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResponse;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResult;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Single responsibility: Orchestrate the microloan calculation use-case
 * (Redis caching, calculation, persistence of schedule in MongoDB, and history recording).
 */
@Service
@RequiredArgsConstructor
public class MicroloanServiceImpl implements MicroloanService {

    private final MicroloanCalculator microloanCalculator;
    private final PaymentScheduleService paymentScheduleService;
    private final CalculationHistoryService historyService;
    private final CalculationCacheKeyGenerator cacheKeyGenerator;
    private final MicroloanCacheService cacheService;
    private final MicroloanConfigService configService;
    private final MicroloanRateService microloanRateService;

    @Override
    public MicroloanCalculationResponse calculateAndPersist(MicroloanCalculationRequest request, UUID userId) {
        var config = configService.getConfig();
        var microloanRate = microloanRateService.findByName(request.rateName());
        String hash = cacheKeyGenerator.generateKey(request, microloanRate.getBaseDailyRate(), config.getBankInterest());

        Optional<MicroloanCalculationResponse> cached = cacheService.getFromCache(hash);
        if (cached.isPresent()) {
            return cached.get();
        }

        MicroloanCalculationResult result = microloanCalculator.calculate(request);

        PaymentScheduleDocument savedSchedule = paymentScheduleService.saveSchedule(result, hash);
        historyService.recordHistory(userId, savedSchedule.getId(), result);

        MicroloanCalculationResponse response = new MicroloanCalculationResponse(
                savedSchedule.getId(),
                result.loanAmount(),
                result.termDays(),
                result.microloanType(),
                result.purpose(),
                result.baseDailyRate(),
                result.vanitySurchargeRate(),
                result.vanitySurchargeAmount(),
                result.effectiveDailyRate(),
                result.annualEquivalentRate(),
                result.totalInterest(),
                result.totalRepayment(),
                result.startDate(),
                result.dueDate(),
                result.currency(),
                result.dailySchedule()
        );

        cacheService.putInCache(hash, response);
        return response;
    }
}
