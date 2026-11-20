package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.calculator.factory.CalculationContextFactory;
import com.surprise.mortgagesurprise.calculator.strategy.microloan.BasicNeedsMicroloanStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.microloan.MicroloanCalculationStrategyRegistry;
import com.surprise.mortgagesurprise.calculator.strategy.microloan.VanityMicroloanStrategy;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResult;
import com.surprise.mortgagesurprise.model.entity.MicroloanConfig;
import com.surprise.mortgagesurprise.model.entity.MicroloanRate;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.MicroloanType;
import com.surprise.mortgagesurprise.service.ExchangeRateService;
import com.surprise.mortgagesurprise.service.MicroloanConfigService;
import com.surprise.mortgagesurprise.service.MicroloanRateService;
import com.surprise.mortgagesurprise.service.impl.MicroloanCalculatorImpl;
import com.surprise.mortgagesurprise.testutil.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("MicroloanCalculator Tests")
class MicroloanCalculatorTest {

    @Mock
    private MicroloanConfigService configService;

    @Mock
    private MicroloanRateService microloanRateService;

    @Mock
    private ExchangeRateService exchangeRateService;

    private MicroloanCalculatorImpl calculator;

    @BeforeEach
    void setUp() {
        CalculationRequestValidator requestValidator = new CalculationRequestValidator();
        CalculationContextFactory contextFactory = new CalculationContextFactory(requestValidator);
        MicroloanCalculationStrategyRegistry strategyRegistry = new MicroloanCalculationStrategyRegistry(List.of(
                new BasicNeedsMicroloanStrategy(),
                new VanityMicroloanStrategy()
        ));
        calculator = new MicroloanCalculatorImpl(contextFactory, strategyRegistry, configService, microloanRateService, exchangeRateService);
    }

    private void mockDefaultRateAndConfig() {
        when(configService.getConfig()).thenReturn(new MicroloanConfig("DEFAULT", TestConstants.BANK_INTEREST));
        when(microloanRateService.findByName(anyString())).thenReturn(
                new MicroloanRate(TestConstants.DEFAULT_RATE_NAME, TestConstants.BASE_DAILY_RATE, 0, Integer.MAX_VALUE)
        );
    }

    @Nested
    @DisplayName("Basic Needs Microloan Calculation")
    class BasicNeedsCalculations {

        @Test
        @DisplayName("Should correctly calculate basic needs microloan schedule and totals")
        void calculate_shouldComputeBasicNeedsTotals_whenStandardRequestProvided() {
            // Arrange
            mockDefaultRateAndConfig();
            LocalDate startDate = LocalDate.of(2026, 12, 1);
            MicroloanCalculationRequest request = new MicroloanCalculationRequest(
                    TestConstants.MICROLOAN_AMOUNT,
                    10,
                    TestConstants.DEFAULT_RATE_NAME,
                    MicroloanType.BASIC_NEEDS,
                    "Продукты питания",
                    null,
                    startDate,
                    Currency.RUB
            );

            // Act
            MicroloanCalculationResult result = calculator.calculate(request);

            // Assert
            assertThat(result.loanAmount()).isEqualByComparingTo("10000.00");
            assertThat(result.termDays()).isEqualTo(10);
            assertThat(result.microloanType()).isEqualTo(MicroloanType.BASIC_NEEDS);
            assertThat(result.baseDailyRate()).isEqualByComparingTo("0.8000");
            assertThat(result.totalInterest()).isEqualByComparingTo("800.00");
            assertThat(result.totalRepayment()).isEqualByComparingTo("10800.00");
            assertThat(result.dueDate()).isEqualTo(LocalDate.of(2026, 12, 11));
            assertThat(result.dailySchedule()).hasSize(10);
        }
    }

    @Nested
    @DisplayName("Vanity Microloan Calculation")
    class VanityCalculations {

        @Test
        @DisplayName("Should include bank interest surcharge for vanity microloan")
        void calculate_shouldIncludeSurcharge_whenVanityTypeExplicitlyProvided() {
            // Arrange
            mockDefaultRateAndConfig();
            LocalDate startDate = LocalDate.of(2026, 12, 1);
            MicroloanCalculationRequest request = new MicroloanCalculationRequest(
                    TestConstants.MICROLOAN_AMOUNT,
                    10,
                    TestConstants.DEFAULT_RATE_NAME,
                    MicroloanType.VANITY,
                    "Покупка нового смартфона",
                    null,
                    startDate,
                    Currency.RUB
            );

            // Act
            MicroloanCalculationResult result = calculator.calculate(request);

            // Assert
            assertThat(result.loanAmount()).isEqualByComparingTo("10000.00");
            assertThat(result.termDays()).isEqualTo(10);
            assertThat(result.microloanType()).isEqualTo(MicroloanType.VANITY);
            assertThat(result.baseDailyRate()).isEqualByComparingTo("0.8000");
            // Base interest = 10,000 * 0.008 * 10 = 800.00, Surcharge = 2000.00 -> Total = 2800.00
            assertThat(result.vanitySurchargeAmount()).isEqualByComparingTo("2000.00");
            assertThat(result.totalInterest()).isEqualByComparingTo("2800.00");
            assertThat(result.totalRepayment()).isEqualByComparingTo("12800.00");
            assertThat(result.dailySchedule()).hasSize(10);
        }

        @Test
        @DisplayName("Should automatically classify as vanity loan when purpose contains vanity keywords")
        void calculate_shouldAutoClassifyAsVanity_whenPurposeContainsVanityKeyword() {
            // Arrange
            mockDefaultRateAndConfig();
            MicroloanCalculationRequest request = new MicroloanCalculationRequest(
                    new BigDecimal("20000.00"),
                    14,
                    TestConstants.DEFAULT_RATE_NAME,
                    null,
                    "Тюнинг автомобиля"
            );

            // Act
            MicroloanCalculationResult result = calculator.calculate(request);

            // Assert
            assertThat(result.microloanType()).isEqualTo(MicroloanType.VANITY);
            // Base interest = 20,000 * 0.008 * 14 = 2240.00 + 2000.00 surcharge = 4240.00
            assertThat(result.vanitySurchargeAmount()).isEqualByComparingTo("2000.00");
            assertThat(result.totalInterest()).isEqualByComparingTo("4240.00");
        }

        @Test
        @DisplayName("Should adapt vanity surcharge when currency is USD based on exchange rates")
        void calculate_shouldAdaptVanitySurcharge_whenCurrencyIsUSD() {
            // Arrange
            mockDefaultRateAndConfig();
            // bankInterest is 2000 RUB, exchange rate for USD is 80.00 RUB/USD -> 2000 / 80 = 25.00 USD
            when(exchangeRateService.getRates()).thenReturn(Map.of(
                    "RUB", BigDecimal.ONE,
                    "USD", new BigDecimal("80.00")
            ));

            MicroloanCalculationRequest request = new MicroloanCalculationRequest(
                    new BigDecimal("5000.00"),
                    10,
                    TestConstants.DEFAULT_RATE_NAME,
                    MicroloanType.VANITY,
                    "VIP отдых",
                    null,
                    LocalDate.now(),
                    Currency.USD
            );

            // Act
            MicroloanCalculationResult result = calculator.calculate(request);

            // Assert
            assertThat(result.microloanType()).isEqualTo(MicroloanType.VANITY);
            assertThat(result.currency()).isEqualTo(Currency.USD);
            assertThat(result.vanitySurchargeAmount()).isEqualByComparingTo("25.00");
            // Base interest = 5,000 * 0.008 * 10 = 400.00 + 25.00 surcharge = 425.00
            assertThat(result.totalInterest()).isEqualByComparingTo("425.00");
        }

        @Test
        @DisplayName("Should adapt vanity surcharge when currency is EUR based on exchange rates")
        void calculate_shouldAdaptVanitySurcharge_whenCurrencyIsEUR() {
            // Arrange
            mockDefaultRateAndConfig();
            // bankInterest is 2000 RUB, exchange rate for EUR is 100.00 RUB/EUR -> 2000 / 100 = 20.00 EUR
            when(exchangeRateService.getRates()).thenReturn(Map.of(
                    "RUB", BigDecimal.ONE,
                    "EUR", new BigDecimal("100.00")
            ));

            MicroloanCalculationRequest request = new MicroloanCalculationRequest(
                    new BigDecimal("5000.00"),
                    10,
                    TestConstants.DEFAULT_RATE_NAME,
                    MicroloanType.VANITY,
                    "Покупка ювелирных изделий",
                    null,
                    LocalDate.now(),
                    Currency.EUR
            );

            // Act
            MicroloanCalculationResult result = calculator.calculate(request);

            // Assert
            assertThat(result.currency()).isEqualTo(Currency.EUR);
            assertThat(result.vanitySurchargeAmount()).isEqualByComparingTo("20.00");
        }
    }

    @Nested
    @DisplayName("Validation and Error Paths")
    class ValidationAndErrors {

        @ParameterizedTest(name = "Invalid term days: {0}")
        @ValueSource(ints = {6, 31, 0, -5})
        @DisplayName("Should throw IllegalArgumentException when term days is outside [7, 30]")
        void calculate_shouldThrowIllegalArgumentException_whenTermDaysOutOfBounds(int invalidDays) {
            // Arrange
            mockDefaultRateAndConfig();
            MicroloanCalculationRequest request = new MicroloanCalculationRequest(
                    new BigDecimal("5000.00"),
                    invalidDays,
                    TestConstants.DEFAULT_RATE_NAME,
                    MicroloanType.BASIC_NEEDS,
                    "Тест"
            );

            // Act & Assert
            assertThatThrownBy(() -> calculator.calculate(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 7 and 30 days");
        }

        @ParameterizedTest(name = "Invalid loan amount: {0}")
        @ValueSource(strings = {"500.00", "999.99", "100000.01", "200000.00"})
        @DisplayName("Should throw IllegalArgumentException when loan amount is outside [1000, 100000]")
        void calculate_shouldThrowIllegalArgumentException_whenLoanAmountOutOfBounds(String invalidAmount) {
            // Arrange
            mockDefaultRateAndConfig();
            MicroloanCalculationRequest request = new MicroloanCalculationRequest(
                    new BigDecimal(invalidAmount),
                    14,
                    TestConstants.DEFAULT_RATE_NAME,
                    MicroloanType.BASIC_NEEDS,
                    "Тест"
            );

            // Act & Assert
            assertThatThrownBy(() -> calculator.calculate(request))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when request is null")
        void calculate_shouldThrowIllegalArgumentException_whenRequestIsNull() {
            // Arrange & Act & Assert
            assertThatThrownBy(() -> calculator.calculate(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Microloan calculation request cannot be null");
        }
    }
}
