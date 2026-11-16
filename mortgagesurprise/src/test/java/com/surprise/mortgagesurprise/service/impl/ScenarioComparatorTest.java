package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.scenario.ScenarioComparisonResponse;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.model.entity.CalculationScenario;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.service.ScenarioComparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScenarioComparatorTest {

    private ScenarioComparator comparator;
    private UUID userId;

    @BeforeEach
    void setUp() {
        comparator = new ScenarioComparator();
        userId = UUID.randomUUID();
    }

    private CalculationScenario createScenario(UUID id, String title, BigDecimal monthlyPayment, BigDecimal totalInterest) {
        CalculationScenario scenario = new CalculationScenario(
                userId,
                title,
                new BigDecimal("6000000"),
                new BigDecimal("1200000"),
                new BigDecimal("4800000"),
                240,
                new BigDecimal("10.0"),
                PaymentType.ANNUITY,
                monthlyPayment,
                totalInterest,
                new BigDecimal("8000000"),
                "schedule-id"
        );
        try {
            var field = CalculationScenario.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(scenario, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return scenario;
    }

    @Test
    @DisplayName("Compare identifies scenario with lowest monthly payment and lowest overpayment")
    void testCompareSuccess() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        CalculationScenario s1 = createScenario(id1, "Plan A", new BigDecimal("40000.00"), new BigDecimal("3000000.00"));
        CalculationScenario s2 = createScenario(id2, "Plan B", new BigDecimal("55000.00"), new BigDecimal("1500000.00"));

        ScenarioComparisonResponse result = comparator.compare(List.of(s1, s2));

        assertThat(result).isNotNull();
        assertThat(result.scenarios()).hasSize(2);
        assertThat(result.lowestMonthlyPaymentScenarioId()).isEqualTo(id1);
        assertThat(result.lowestOverpaymentScenarioId()).isEqualTo(id2);
    }

    @Test
    @DisplayName("Compare with fewer than 2 scenarios throws BusinessException")
    void testCompareLessThanTwoThrows() {
        CalculationScenario s1 = createScenario(UUID.randomUUID(), "Plan A", new BigDecimal("40000.00"), new BigDecimal("3000000.00"));
        assertThatThrownBy(() -> comparator.compare(List.of(s1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Compare with more than 3 scenarios throws BusinessException")
    void testCompareMoreThanThreeThrows() {
        CalculationScenario s1 = createScenario(UUID.randomUUID(), "Plan 1", new BigDecimal("40000.00"), new BigDecimal("3000000.00"));
        CalculationScenario s2 = createScenario(UUID.randomUUID(), "Plan 2", new BigDecimal("41000.00"), new BigDecimal("3100000.00"));
        CalculationScenario s3 = createScenario(UUID.randomUUID(), "Plan 3", new BigDecimal("42000.00"), new BigDecimal("3200000.00"));
        CalculationScenario s4 = createScenario(UUID.randomUUID(), "Plan 4", new BigDecimal("43000.00"), new BigDecimal("3300000.00"));

        assertThatThrownBy(() -> comparator.compare(List.of(s1, s2, s3, s4)))
                .isInstanceOf(BusinessException.class);
    }
}
