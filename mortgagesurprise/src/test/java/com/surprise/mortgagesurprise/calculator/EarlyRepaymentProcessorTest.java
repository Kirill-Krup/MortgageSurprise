package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.dto.calculator.EarlyRepaymentRequest;
import com.surprise.mortgagesurprise.model.enums.EarlyRepaymentType;
import com.surprise.mortgagesurprise.model.enums.RepaymentStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Tag("unit")
@DisplayName("EarlyRepaymentProcessor Tests")
class EarlyRepaymentProcessorTest {

    private EarlyRepaymentProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new EarlyRepaymentProcessor();
    }

    @Nested
    @DisplayName("Empty or Inactive Repayments")
    class EmptyRepayments {

        @Test
        @DisplayName("Should return zero resolution when repayment list is null, empty, or balance is zero")
        void resolve_shouldReturnZero_whenRepaymentsNullOrEmptyOrBalanceZero() {
            // Arrange
            BigDecimal balance = new BigDecimal("100000.00");
            List<EarlyRepaymentRequest> singleRequest = List.of(
                    new EarlyRepaymentRequest(1, null, new BigDecimal("10000.00"), RepaymentStrategy.REDUCE_TERM, EarlyRepaymentType.ONE_TIME)
            );

            // Act
            EarlyRepaymentResolution resNull = processor.resolve(null, 1, balance);
            EarlyRepaymentResolution resEmpty = processor.resolve(List.of(), 1, balance);
            EarlyRepaymentResolution resZeroBalance = processor.resolve(singleRequest, 1, BigDecimal.ZERO);

            // Assert
            assertAll("Zero resolutions",
                    () -> assertThat(resNull.actualAmount()).isEqualByComparingTo("0.00"),
                    () -> assertThat(resEmpty.actualAmount()).isEqualByComparingTo("0.00"),
                    () -> assertThat(resZeroBalance.actualAmount()).isEqualByComparingTo("0.00")
            );
        }
    }

    @Nested
    @DisplayName("One-Time Early Repayments")
    class OneTimeRepayments {

        @Test
        @DisplayName("Should resolve early repayment only on the exact target month")
        void resolve_shouldApplyRepayment_whenTargetMonthMatches() {
            // Arrange
            List<EarlyRepaymentRequest> requests = List.of(
                    new EarlyRepaymentRequest(3, null, new BigDecimal("50000.00"), RepaymentStrategy.REDUCE_TERM, EarlyRepaymentType.ONE_TIME)
            );
            BigDecimal balance = new BigDecimal("200000.00");

            // Act
            EarlyRepaymentResolution month2 = processor.resolve(requests, 2, balance);
            EarlyRepaymentResolution month3 = processor.resolve(requests, 3, balance);
            EarlyRepaymentResolution month4 = processor.resolve(requests, 4, balance);

            // Assert
            assertThat(month2.actualAmount()).isEqualByComparingTo("0.00");
            assertThat(month3.actualAmount()).isEqualByComparingTo("50000.00");
            assertThat(month3.strategy()).isEqualTo(RepaymentStrategy.REDUCE_TERM);
            assertThat(month4.actualAmount()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("Recurring Early Repayments")
    class RecurringRepayments {

        @Test
        @DisplayName("Should resolve repayment for every month in start to end range")
        void resolve_shouldApplyRepayment_whenMonthWithinStartAndEndRange() {
            // Arrange
            List<EarlyRepaymentRequest> requests = List.of(
                    new EarlyRepaymentRequest(2, 4, new BigDecimal("25000.00"), RepaymentStrategy.REDUCE_PAYMENT, EarlyRepaymentType.RECURRING)
            );
            BigDecimal balance = new BigDecimal("200000.00");

            // Act & Assert
            assertThat(processor.resolve(requests, 1, balance).actualAmount()).isEqualByComparingTo("0.00");
            assertThat(processor.resolve(requests, 2, balance).actualAmount()).isEqualByComparingTo("25000.00");
            assertThat(processor.resolve(requests, 3, balance).actualAmount()).isEqualByComparingTo("25000.00");
            assertThat(processor.resolve(requests, 4, balance).actualAmount()).isEqualByComparingTo("25000.00");
            assertThat(processor.resolve(requests, 5, balance).actualAmount()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("Balance Capping")
    class BalanceCapping {

        @Test
        @DisplayName("Should cap early repayment amount to available loan balance")
        void resolve_shouldCapAmount_whenRepaymentExceedsAvailableBalance() {
            // Arrange
            List<EarlyRepaymentRequest> requests = List.of(
                    new EarlyRepaymentRequest(1, null, new BigDecimal("100000.00"), RepaymentStrategy.REDUCE_TERM, EarlyRepaymentType.ONE_TIME)
            );
            BigDecimal smallBalance = new BigDecimal("35000.00");

            // Act
            EarlyRepaymentResolution result = processor.resolve(requests, 1, smallBalance);

            // Assert
            assertThat(result.actualAmount()).isEqualByComparingTo("35000.00");
            assertThat(result.strategy()).isEqualTo(RepaymentStrategy.REDUCE_TERM);
        }
    }
}
