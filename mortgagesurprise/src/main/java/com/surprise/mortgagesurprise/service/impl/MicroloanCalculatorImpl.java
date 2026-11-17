package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.calculator.factory.CalculationContextFactory;
import com.surprise.mortgagesurprise.calculator.factory.MicroloanCalculationContext;
import com.surprise.mortgagesurprise.calculator.strategy.microloan.MicroloanCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.microloan.MicroloanCalculationStrategyRegistry;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResult;
import com.surprise.mortgagesurprise.model.entity.MicroloanConfig;
import com.surprise.mortgagesurprise.service.MicroloanCalculator;
import com.surprise.mortgagesurprise.service.MicroloanConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Single responsibility: Orchestrate microloan calculations using admin-configured rates,
 * CalculationContextFactory for parameter preparation, and strategy dispatch by MicroloanType.
 */
@Component
@RequiredArgsConstructor
public class MicroloanCalculatorImpl implements MicroloanCalculator {

    private final CalculationContextFactory contextFactory;
    private final MicroloanCalculationStrategyRegistry strategyRegistry;
    private final MicroloanConfigService configService;

    @Override
    public MicroloanCalculationResult calculate(MicroloanCalculationRequest request) {
        MicroloanConfig config = configService.getConfig();
        MicroloanCalculationContext context = contextFactory.createMicroloanContext(
                request,
                config.getBaseDailyRate(),
                config.getVanitySurchargeRate()
        );
        MicroloanCalculationStrategy strategy = strategyRegistry.getStrategy(context.microloanType());
        return strategy.calculate(context);
    }
}
