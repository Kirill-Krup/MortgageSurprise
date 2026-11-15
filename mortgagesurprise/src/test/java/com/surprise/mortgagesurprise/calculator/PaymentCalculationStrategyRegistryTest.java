package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.calculator.strategy.AnnuityPaymentCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.DifferentiatedPaymentCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.PaymentCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.PaymentCalculationStrategyRegistry;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentCalculationStrategyRegistryTest {

    private PaymentCalculationStrategyRegistry registry;

    @BeforeEach
    void setUp() {
        EarlyRepaymentProcessor processor = new EarlyRepaymentProcessor();
        registry = new PaymentCalculationStrategyRegistry(List.of(
                new AnnuityPaymentCalculationStrategy(processor),
                new DifferentiatedPaymentCalculationStrategy(processor)
        ));
    }

    @Test
    @DisplayName("Registry retrieves correct strategy for ANNUITY")
    void testGetAnnuityStrategy() {
        PaymentCalculationStrategy strategy = registry.getStrategy(PaymentType.ANNUITY);
        assertNotNull(strategy);
        assertEquals(PaymentType.ANNUITY, strategy.getSupportedPaymentType());
    }

    @Test
    @DisplayName("Registry retrieves correct strategy for DIFFERENTIATED")
    void testGetDifferentiatedStrategy() {
        PaymentCalculationStrategy strategy = registry.getStrategy(PaymentType.DIFFERENTIATED);
        assertNotNull(strategy);
        assertEquals(PaymentType.DIFFERENTIATED, strategy.getSupportedPaymentType());
    }

    @Test
    @DisplayName("Null payment type throws IllegalArgumentException")
    void testNullPaymentType() {
        assertThrows(IllegalArgumentException.class, () -> registry.getStrategy(null));
    }

    @Test
    @DisplayName("Unsupported payment type throws UnsupportedOperationException")
    void testUnsupportedPaymentType() {
        PaymentCalculationStrategyRegistry emptyRegistry = new PaymentCalculationStrategyRegistry(List.of());
        assertThrows(UnsupportedOperationException.class, () -> emptyRegistry.getStrategy(PaymentType.ANNUITY));
    }
}
