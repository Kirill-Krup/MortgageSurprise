package com.surprise.mortgagesurprise.calculator.strategy;

import com.surprise.mortgagesurprise.model.enums.PaymentType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Single responsibility: Maintain registry of calculation strategies and route requests by PaymentType.
 */
@Component
public class PaymentCalculationStrategyRegistry {

    private final Map<PaymentType, PaymentCalculationStrategy> strategyMap = new EnumMap<>(PaymentType.class);

    public PaymentCalculationStrategyRegistry(List<PaymentCalculationStrategy> strategies) {
        if (strategies != null) {
            for (PaymentCalculationStrategy strategy : strategies) {
                strategyMap.put(strategy.getSupportedPaymentType(), strategy);
            }
        }
    }

    public PaymentCalculationStrategy getStrategy(PaymentType paymentType) {
        if (paymentType == null) {
            throw new IllegalArgumentException("Payment type cannot be null");
        }
        PaymentCalculationStrategy strategy = strategyMap.get(paymentType);
        if (strategy == null) {
            throw new UnsupportedOperationException("No calculation strategy found for payment type: " + paymentType);
        }
        return strategy;
    }
}
