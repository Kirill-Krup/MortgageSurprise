package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.calculator.factory.CalculationContextFactory;
import com.surprise.mortgagesurprise.calculator.strategy.AnnuityPaymentCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.DifferentiatedPaymentCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.PaymentCalculationStrategyRegistry;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.dto.calculator.EarlyRepaymentRequest;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.EarlyRepaymentType;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.model.enums.RepaymentStrategy;
import com.surprise.mortgagesurprise.service.MortgageCalculator;
import com.surprise.mortgagesurprise.service.impl.MortgageCalculatorImpl;
import com.surprise.mortgagesurprise.testutil.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("MortgageCalculator Tests")
class MortgageCalculatorTest {

    private MortgageCalculator calculator;

    @BeforeEach
    void setUp() {
        CalculationRequestValidator validator = new CalculationRequestValidator();
        CalculationContextFactory factory = new CalculationContextFactory(validator);
        EarlyRepaymentProcessor earlyRepaymentProcessor = new EarlyRepaymentProcessor();
        PaymentCalculationStrategyRegistry registry = new PaymentCalculationStrategyRegistry(List.of(
                new AnnuityPaymentCalculationStrategy(earlyRepaymentProcessor),
                new DifferentiatedPaymentCalculationStrategy(earlyRepaymentProcessor)
        ));

        calculator = new MortgageCalculatorImpl(factory, registry);
    }

    @Nested
    @DisplayName("Annuity Mortgage Calculation")
    class AnnuityCalculations {

        @Test
        @DisplayName("Should correctly calculate annuity mortgage matching standard bank formula")
        void calculate_shouldComputeStandardAnnuity_whenValidInputsProvided() {
            // Arrange
            CalculationRequest request = new CalculationRequest(
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.DOWN_PAYMENT,
                    TestConstants.TERM_MONTHS,
                    TestConstants.ANNUAL_RATE,
                    PaymentType.ANNUITY,
                    LocalDate.of(2026, 1, 1),
                    List.of()
            );

            // Act
            CalculationResult result = calculator.calculate(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.loanAmount()).isEqualByComparingTo("4000000.00");
            assertThat(result.actualTermMonths()).isEqualTo(240);
            assertThat(result.monthlyPayment()).isEqualByComparingTo("38600.87");

            var lastItem = result.schedule().getLast();
            assertThat(lastItem.closingBalance()).isEqualByComparingTo("0.00");

            assertThat(result.totalInterest())
                    .isGreaterThan(new BigDecimal("5200000.00"))
                    .isLessThan(new BigDecimal("5300000.00"));
            assertThat(result.totalPayment())
                    .isEqualByComparingTo(result.loanAmount().add(result.totalInterest()));
        }
    }

    @Nested
    @DisplayName("Differentiated Mortgage Calculation")
    class DifferentiatedCalculations {

        @Test
        @DisplayName("Should correctly compute differentiated mortgage with declining monthly payments")
        void calculate_shouldComputeDifferentiatedPayments_whenDifferentiatedTypeProvided() {
            // Arrange
            CalculationRequest request = new CalculationRequest(
                    new BigDecimal("1500000.00"),
                    new BigDecimal("300000.00"),
                    12,
                    new BigDecimal("12.00"),
                    PaymentType.DIFFERENTIATED,
                    LocalDate.of(2026, 1, 1),
                    List.of()
            );

            // Act
            CalculationResult result = calculator.calculate(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.actualTermMonths()).isEqualTo(12);
            assertThat(result.schedule().getFirst().totalMonthlyPayment()).isEqualByComparingTo("112000.00");
            assertThat(result.schedule().getLast().totalMonthlyPayment()).isEqualByComparingTo("101000.00");
            assertThat(result.schedule().getLast().closingBalance()).isEqualByComparingTo("0.00");
            assertThat(result.totalInterest()).isEqualByComparingTo("78000.00");
        }
    }

    @Nested
    @DisplayName("Early Repayment Scenarios")
    class EarlyRepaymentScenarios {

        @Test
        @DisplayName("Should reduce total loan term when early repayment uses REDUCE_TERM strategy")
        void calculate_shouldReduceTerm_whenReduceTermEarlyRepaymentApplied() {
            // Arrange
            CalculationRequest request = new CalculationRequest(
                    new BigDecimal("1200000.00"),
                    new BigDecimal("200000.00"),
                    120,
                    new BigDecimal("12.00"),
                    PaymentType.ANNUITY,
                    LocalDate.of(2026, 1, 1),
                    List.of(
                            new EarlyRepaymentRequest(
                                    6,
                                    null,
                                    new BigDecimal("300000.00"),
                                    RepaymentStrategy.REDUCE_TERM,
                                    EarlyRepaymentType.ONE_TIME
                            )
                    )
            );

            // Act
            CalculationResult resultWithEarly = calculator.calculate(request);

            // Assert
            assertThat(resultWithEarly.actualTermMonths()).isLessThan(120);
            assertThat(resultWithEarly.schedule().getLast().closingBalance()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Should lower future payments while preserving term when early repayment uses REDUCE_PAYMENT")
        void calculate_shouldLowerMonthlyPayment_whenReducePaymentEarlyRepaymentApplied() {
            // Arrange
            CalculationRequest baselineRequest = new CalculationRequest(
                    new BigDecimal("1200000.00"),
                    new BigDecimal("200000.00"),
                    120,
                    new BigDecimal("12.00"),
                    PaymentType.ANNUITY,
                    LocalDate.of(2026, 1, 1),
                    List.of()
            );

            CalculationRequest earlyRequest = new CalculationRequest(
                    new BigDecimal("1200000.00"),
                    new BigDecimal("200000.00"),
                    120,
                    new BigDecimal("12.00"),
                    PaymentType.ANNUITY,
                    LocalDate.of(2026, 1, 1),
                    List.of(
                            new EarlyRepaymentRequest(
                                    6,
                                    null,
                                    new BigDecimal("300000.00"),
                                    RepaymentStrategy.REDUCE_PAYMENT,
                                    EarlyRepaymentType.ONE_TIME
                            )
                    )
            );

            // Act
            CalculationResult baseline = calculator.calculate(baselineRequest);
            CalculationResult withEarly = calculator.calculate(earlyRequest);

            // Assert
            assertThat(withEarly.actualTermMonths()).isEqualTo(120);
            BigDecimal paymentMonth5 = withEarly.schedule().get(4).totalMonthlyPayment();
            BigDecimal paymentMonth7 = withEarly.schedule().get(6).totalMonthlyPayment();
            assertThat(paymentMonth7).isLessThan(paymentMonth5);
            assertThat(withEarly.totalInterest()).isLessThan(baseline.totalInterest());
        }
    }

    @Nested
    @DisplayName("Currency and Validation")
    class CurrencyAndValidation {

        @ParameterizedTest(name = "Currency: {0}")
        @EnumSource(Currency.class)
        @DisplayName("Should preserve requested currency in calculation result")
        void calculate_shouldPreserveCurrency_whenCurrencySpecified(Currency currency) {
            // Arrange
            CalculationRequest request = new CalculationRequest(
                    new BigDecimal("300000.00"),
                    new BigDecimal("60000.00"),
                    180,
                    new BigDecimal("6.50"),
                    PaymentType.ANNUITY,
                    currency,
                    LocalDate.now(),
                    List.of()
            );

            // Act
            CalculationResult result = calculator.calculate(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.currency()).isEqualTo(currency);
            assertThat(result.loanAmount()).isEqualByComparingTo("240000.00");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when down payment is equal to or greater than property price")
        void calculate_shouldThrowIllegalArgumentException_whenDownPaymentEqualsPropertyPrice() {
            // Arrange
            CalculationRequest request = new CalculationRequest(
                    new BigDecimal("1000000.00"),
                    new BigDecimal("1000000.00"),
                    120,
                    new BigDecimal("10.00"),
                    PaymentType.ANNUITY,
                    LocalDate.now(),
                    List.of()
            );

            assertThatThrownBy(() -> calculator.calculate(request))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
