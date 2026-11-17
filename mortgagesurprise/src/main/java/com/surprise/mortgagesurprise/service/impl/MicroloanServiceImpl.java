package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.calculator.CalculationCacheKeyGenerator;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResponse;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResult;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.model.entity.MicroloanConfig;
import com.surprise.mortgagesurprise.service.CalculationHistoryService;
import com.surprise.mortgagesurprise.service.MicroloanCacheService;
import com.surprise.mortgagesurprise.service.MicroloanCalculator;
import com.surprise.mortgagesurprise.service.MicroloanConfigService;
import com.surprise.mortgagesurprise.service.MicroloanService;
import com.surprise.mortgagesurprise.service.PaymentScheduleService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Single responsibility: Orchestrate the microloan calculation use-case
 * (Redis caching, calculation, persistence of schedule in MongoDB, and history recording).
 */
@Service
public class MicroloanServiceImpl implements MicroloanService {

    private final MicroloanCalculator microloanCalculator;
    private final PaymentScheduleService paymentScheduleService;
    private final CalculationHistoryService historyService;
    private final CalculationCacheKeyGenerator cacheKeyGenerator;
    private final MicroloanCacheService cacheService;
    private final MicroloanConfigService configService;

    public MicroloanServiceImpl(MicroloanCalculator microloanCalculator,
                                PaymentScheduleService paymentScheduleService,
                                CalculationHistoryService historyService,
                                CalculationCacheKeyGenerator cacheKeyGenerator,
                                MicroloanCacheService cacheService,
                                MicroloanConfigService configService) {
        this.microloanCalculator = microloanCalculator;
        this.paymentScheduleService = paymentScheduleService;
        this.historyService = historyService;
        this.cacheKeyGenerator = cacheKeyGenerator;
        this.cacheService = cacheService;
        this.configService = configService;
    }

    @Override
    public MicroloanCalculationResponse calculateAndPersist(MicroloanCalculationRequest request, UUID userId) {
        MicroloanConfig config = configService.getConfig();
        String hash = cacheKeyGenerator.generateKey(request, config.getBaseDailyRate(), config.getVanitySurchargeRate());

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
