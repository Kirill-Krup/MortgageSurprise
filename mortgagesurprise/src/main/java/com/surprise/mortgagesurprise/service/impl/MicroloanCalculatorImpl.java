package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.calculator.factory.CalculationContextFactory;
import com.surprise.mortgagesurprise.calculator.factory.MicroloanCalculationContext;
import com.surprise.mortgagesurprise.calculator.strategy.microloan.MicroloanCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.microloan.MicroloanCalculationStrategyRegistry;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResult;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.service.ExchangeRateService;
import com.surprise.mortgagesurprise.service.MicroloanCalculator;
import com.surprise.mortgagesurprise.service.MicroloanConfigService;
import com.surprise.mortgagesurprise.service.MicroloanRateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
@Slf4j
public class MicroloanCalculatorImpl implements MicroloanCalculator {

    private final CalculationContextFactory contextFactory;
    private final MicroloanCalculationStrategyRegistry strategyRegistry;
    private final MicroloanConfigService configService;
    private final MicroloanRateService microloanRateService;
    private final ExchangeRateService exchangeRateService;

    @org.springframework.beans.factory.annotation.Autowired
    public MicroloanCalculatorImpl(
            CalculationContextFactory contextFactory,
            MicroloanCalculationStrategyRegistry strategyRegistry,
            MicroloanConfigService configService,
            MicroloanRateService microloanRateService,
            ExchangeRateService exchangeRateService) {
        this.contextFactory = contextFactory;
        this.strategyRegistry = strategyRegistry;
        this.configService = configService;
        this.microloanRateService = microloanRateService;
        this.exchangeRateService = exchangeRateService;
    }

    public MicroloanCalculatorImpl(
            CalculationContextFactory contextFactory,
            MicroloanCalculationStrategyRegistry strategyRegistry,
            MicroloanConfigService configService,
            MicroloanRateService microloanRateService) {
        this(contextFactory, strategyRegistry, configService, microloanRateService, null);
    }

    @Override
    public MicroloanCalculationResult calculate(MicroloanCalculationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Microloan calculation request cannot be null");
        }
        var config = configService.getConfig();
        var microloanRate = microloanRateService.findByName(request.rateName());
        BigDecimal bankInterest = config.getBankInterest();

        if (bankInterest != null && request.currency() != null && request.currency() != Currency.RUB && exchangeRateService != null) {
            Map<String, BigDecimal> rates = exchangeRateService.getRates();
            if (rates != null && rates.containsKey(request.currency().name())) {
                BigDecimal rate = rates.get(request.currency().name());
                if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                    bankInterest = bankInterest.divide(rate, 2, RoundingMode.HALF_UP);
                    log.debug("Adapted bankInterest to currency {}: rate={}, adapted={}",
                            request.currency(), rate, bankInterest);
                }
            }
        }

        MicroloanCalculationContext context = contextFactory.createMicroloanContext(
                request,
                microloanRate.getBaseDailyRate(),
                bankInterest
        );
        MicroloanCalculationStrategy strategy = strategyRegistry.getStrategy(context.microloanType());
        return strategy.calculate(context);
    }
}
