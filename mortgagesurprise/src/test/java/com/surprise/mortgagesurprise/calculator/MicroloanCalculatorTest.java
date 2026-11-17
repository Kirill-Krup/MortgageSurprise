package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.calculator.factory.CalculationContextFactory;
import com.surprise.mortgagesurprise.calculator.strategy.microloan.BasicNeedsMicroloanStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.microloan.MicroloanCalculationStrategyRegistry;
import com.surprise.mortgagesurprise.calculator.strategy.microloan.VanityMicroloanStrategy;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResult;
import com.surprise.mortgagesurprise.model.entity.MicroloanConfig;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.MicroloanType;
import com.surprise.mortgagesurprise.service.MicroloanConfigService;
import com.surprise.mortgagesurprise.service.impl.MicroloanCalculatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MicroloanCalculatorTest {

    private MicroloanCalculatorImpl calculator;
    private CalculationContextFactory factory;
    private MicroloanConfigService configService;

    @BeforeEach
    void setUp() {
        factory = new CalculationContextFactory(new CalculationRequestValidator());
        MicroloanCalculationStrategyRegistry registry = new MicroloanCalculationStrategyRegistry(
                List.of(new BasicNeedsMicroloanStrategy(), new VanityMicroloanStrategy())
        );
        configService = mock(MicroloanConfigService.class);
        when(configService.getConfig()).thenReturn(new MicroloanConfig(
                "DEFAULT",
                new BigDecimal("0.8000"),
                new BigDecimal("5.0000"),
                Instant.now()
        ));
        calculator = new MicroloanCalculatorImpl(factory, registry, configService);
    }

    @Test
    @DisplayName("Should calculate basic needs microloan with standard daily rate (0.8%)")
    void shouldCalculateBasicNeedsMicroloan() {
        LocalDate startDate = LocalDate.of(2026, 12, 1);
        MicroloanCalculationRequest request = new MicroloanCalculationRequest(
                new BigDecimal("10000.00"),
                10,
                MicroloanType.BASIC_NEEDS,
                "Продукты питания",
                null,
                startDate,
                Currency.RUB
        );

        MicroloanCalculationResult result = calculator.calculate(request);

        assertThat(result.loanAmount()).isEqualByComparingTo("10000.00");
        assertThat(result.termDays()).isEqualTo(10);
        assertThat(result.microloanType()).isEqualTo(MicroloanType.BASIC_NEEDS);
        assertThat(result.baseDailyRate()).isEqualByComparingTo("0.8000");
        assertThat(result.vanitySurchargeRate()).isEqualByComparingTo("0.0000");
        assertThat(result.vanitySurchargeAmount()).isEqualByComparingTo("0.00");
        assertThat(result.effectiveDailyRate()).isEqualByComparingTo("0.8000");
        assertThat(result.totalInterest()).isEqualByComparingTo("800.00");
        assertThat(result.totalRepayment()).isEqualByComparingTo("10800.00");
        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2026, 12, 11));
        assertThat(result.dailySchedule()).hasSize(10);
    }

    @Test
    @DisplayName("Should calculate vanity microloan with base daily rate (0.8%) + fixed surcharge rate (5.0%)")
    void shouldCalculateVanityMicroloan() {
        LocalDate startDate = LocalDate.of(2026, 12, 1);
        MicroloanCalculationRequest request = new MicroloanCalculationRequest(
                new BigDecimal("10000.00"),
                10,
                MicroloanType.VANITY,
                "Покупка нового iPhone",
                null,
                startDate,
                Currency.RUB
        );

        MicroloanCalculationResult result = calculator.calculate(request);

        assertThat(result.loanAmount()).isEqualByComparingTo("10000.00");
        assertThat(result.termDays()).isEqualTo(10);
        assertThat(result.microloanType()).isEqualTo(MicroloanType.VANITY);
        assertThat(result.baseDailyRate()).isEqualByComparingTo("0.8000");
        assertThat(result.vanitySurchargeRate()).isEqualByComparingTo("5.0000");
        // Base interest = 10,000 * 0.008 * 10 = 800.00
        // Fixed surcharge = 10,000 * 0.05 = 500.00
        // Total interest = 800 + 500 = 1300.00, Total repayment = 11300.00
        assertThat(result.vanitySurchargeAmount()).isEqualByComparingTo("500.00");
        assertThat(result.totalInterest()).isEqualByComparingTo("1300.00");
        assertThat(result.totalRepayment()).isEqualByComparingTo("11300.00");
        assertThat(result.dailySchedule()).hasSize(10);
    }

    @Test
    @DisplayName("Should automatically classify vanity loan based on purpose keywords")
    void shouldAutoClassifyVanityLoan() {
        MicroloanCalculationRequest request = new MicroloanCalculationRequest(
                new BigDecimal("20000.00"),
                14,
                null,
                "Тюнинг автомобиля"
        );

        MicroloanCalculationResult result = calculator.calculate(request);
        assertThat(result.microloanType()).isEqualTo(MicroloanType.VANITY);
        // Base interest = 20,000 * 0.008 * 14 = 2240.00
        // Fixed surcharge = 20,000 * 0.05 = 1000.00
        assertThat(result.vanitySurchargeAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.totalInterest()).isEqualByComparingTo("3240.00");
    }

    @Test
    @DisplayName("Should reject invalid term days (< 7 or > 30)")
    void shouldRejectInvalidTermDays() {
        MicroloanCalculationRequest requestTooShort = new MicroloanCalculationRequest(
                new BigDecimal("5000.00"),
                6,
                MicroloanType.BASIC_NEEDS,
                "Тест"
        );
        assertThatThrownBy(() -> calculator.calculate(requestTooShort))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 7 and 30 days");

        MicroloanCalculationRequest requestTooLong = new MicroloanCalculationRequest(
                new BigDecimal("5000.00"),
                31,
                MicroloanType.BASIC_NEEDS,
                "Тест"
        );
        assertThatThrownBy(() -> calculator.calculate(requestTooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 7 and 30 days");
    }

    @Test
    @DisplayName("Should reject invalid loan amount (< 1,000 or > 100,000)")
    void shouldRejectInvalidAmount() {
        MicroloanCalculationRequest requestTooSmall = new MicroloanCalculationRequest(
                new BigDecimal("500.00"),
                14,
                MicroloanType.BASIC_NEEDS,
                "Тест"
        );
        assertThatThrownBy(() -> calculator.calculate(requestTooSmall))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 1,000");

        MicroloanCalculationRequest requestTooLarge = new MicroloanCalculationRequest(
                new BigDecimal("150000.00"),
                14,
                MicroloanType.BASIC_NEEDS,
                "Тест"
        );
        assertThatThrownBy(() -> calculator.calculate(requestTooLarge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 100,000");
    }
}
