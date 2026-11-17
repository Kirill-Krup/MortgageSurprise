package com.surprise.mortgagesurprise.calculator.strategy.microloan;

import com.surprise.mortgagesurprise.model.enums.MicroloanType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy registry for microloan calculations keyed by MicroloanType.
 */
@Component
public class MicroloanCalculationStrategyRegistry {

    private final Map<MicroloanType, MicroloanCalculationStrategy> strategies = new EnumMap<>(MicroloanType.class);

    public MicroloanCalculationStrategyRegistry(List<MicroloanCalculationStrategy> strategyList) {
        if (strategyList != null) {
            for (MicroloanCalculationStrategy s : strategyList) {
                strategies.put(s.getSupportedType(), s);
            }
        }
    }

    public MicroloanCalculationStrategy getStrategy(MicroloanType type) {
        MicroloanType lookup = type != null ? type : MicroloanType.BASIC_NEEDS;
        MicroloanCalculationStrategy strategy = strategies.get(lookup);
        if (strategy == null) {
            throw new IllegalArgumentException("No calculation strategy registered for microloan type: " + lookup);
        }
        return strategy;
    }
}
