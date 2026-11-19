package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.calculator.factory.CalculationContextFactory;
import com.surprise.mortgagesurprise.calculator.factory.MicroloanCalculationContext;
import com.surprise.mortgagesurprise.calculator.strategy.microloan.MicroloanCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.microloan.MicroloanCalculationStrategyRegistry;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResult;
import com.surprise.mortgagesurprise.service.MicroloanCalculator;
import com.surprise.mortgagesurprise.service.MicroloanConfigService;
import com.surprise.mortgagesurprise.service.MicroloanRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class MicroloanCalculatorImpl implements MicroloanCalculator {

    private final CalculationContextFactory contextFactory;
    private final MicroloanCalculationStrategyRegistry strategyRegistry;
    private final MicroloanConfigService configService;
    private final MicroloanRateService microloanRateService;

    @Override
    public MicroloanCalculationResult calculate(MicroloanCalculationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Microloan calculation request cannot be null");
        }
        var config = configService.getConfig();
        var microloanRate = microloanRateService.findByName(request.rateName());
        BigDecimal bankInterest = config.getBankInterest(); // TODO change to currency dependent variant
        MicroloanCalculationContext context = contextFactory.createMicroloanContext(
                request,
                microloanRate.getBaseDailyRate(),
                bankInterest
        );
        MicroloanCalculationStrategy strategy = strategyRegistry.getStrategy(context.microloanType());
        return strategy.calculate(context);
    }
}
