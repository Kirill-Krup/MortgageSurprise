package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.calculator.strategy.AnnuityPaymentCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.DifferentiatedPaymentCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.PaymentCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.PaymentCalculationStrategyRegistry;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("PaymentCalculationStrategyRegistry Tests")
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

    @Nested
    @DisplayName("Strategy Retrieval")
    class StrategyRetrieval {

        @ParameterizedTest(name = "Payment Type: {0}")
        @EnumSource(PaymentType.class)
        @DisplayName("Should retrieve registered strategy corresponding to payment type")
        void getStrategy_shouldReturnMatchingStrategy_whenPaymentTypeSupported(PaymentType paymentType) {
            // Arrange & Act
            PaymentCalculationStrategy strategy = registry.getStrategy(paymentType);

            // Assert
            assertThat(strategy).isNotNull();
            assertThat(strategy.getSupportedPaymentType()).isEqualTo(paymentType);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw IllegalArgumentException when payment type is null")
        void getStrategy_shouldThrowIllegalArgumentException_whenPaymentTypeIsNull() {
            // Arrange & Act & Assert
            assertThatThrownBy(() -> registry.getStrategy(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw UnsupportedOperationException when strategy is not registered")
        void getStrategy_shouldThrowUnsupportedOperationException_whenStrategyNotRegistered() {
            PaymentCalculationStrategyRegistry emptyRegistry = new PaymentCalculationStrategyRegistry(List.of());
            assertThatThrownBy(() -> emptyRegistry.getStrategy(PaymentType.ANNUITY))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("No calculation strategy found for payment type: ANNUITY");
        }
    }
}
