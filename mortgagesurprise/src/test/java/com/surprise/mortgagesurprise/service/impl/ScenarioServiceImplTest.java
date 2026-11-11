package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.scenario.ScenarioComparisonResponse;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.model.entity.CalculationScenario;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.repository.jpa.CalculationScenarioRepository;
import com.surprise.mortgagesurprise.repository.mongo.PaymentScheduleMongoRepository;
import com.surprise.mortgagesurprise.service.MortgageCalculator;
import com.surprise.mortgagesurprise.messaging.producer.MessageProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScenarioServiceImplTest {

    @Mock
    private CalculationScenarioRepository scenarioRepository;

    @Mock
    private MortgageCalculator mortgageCalculator;

    @Mock
    private PaymentScheduleMongoRepository scheduleMongoRepository;

    @Mock
    private MessageProducer messageProducer;

    @InjectMocks
    private ScenarioServiceImpl scenarioService;

    private UUID userId;
    private CalculationScenario scenario1;
    private CalculationScenario scenario2;
    private CalculationScenario scenario3;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        scenario1 = new CalculationScenario(
                userId,
                "Сценарий на 25 лет",
                new BigDecimal("6000000"),
                new BigDecimal("1200000"),
                new BigDecimal("4800000"),
                300,
                new BigDecimal("9.5"),
                PaymentType.ANNUITY,
                new BigDecimal("40000.00"),
                new BigDecimal("3000000.00"),
                new BigDecimal("7800000.00"),
                "mongo-doc-1"
        );
        setId(scenario1, UUID.randomUUID());

        scenario2 = new CalculationScenario(
                userId,
                "Сценарий на 15 лет",
                new BigDecimal("6000000"),
                new BigDecimal("1200000"),
                new BigDecimal("4800000"),
                180,
                new BigDecimal("9.0"),
                PaymentType.ANNUITY,
                new BigDecimal("55000.00"),
                new BigDecimal("1800000.00"),
                new BigDecimal("6600000.00"),
                "mongo-doc-2"
        );
        setId(scenario2, UUID.randomUUID());

        scenario3 = new CalculationScenario(
                userId,
                "Сценарий на 20 лет",
                new BigDecimal("6000000"),
                new BigDecimal("1200000"),
                new BigDecimal("4800000"),
                240,
                new BigDecimal("9.2"),
                PaymentType.ANNUITY,
                new BigDecimal("48000.00"),
                new BigDecimal("2200000.00"),
                new BigDecimal("7000000.00"),
                "mongo-doc-3"
        );
        setId(scenario3, UUID.randomUUID());
    }

    private void setId(CalculationScenario scenario, UUID id) {
        try {
            var field = CalculationScenario.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(scenario, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Сравнение 2 сценариев: корректно определяет минимальный платеж и минимальную переплату")
    void compareScenarios_twoScenarios_success() {
        List<UUID> ids = List.of(scenario1.getId(), scenario2.getId());
        when(scenarioRepository.findAllByIdInAndUserId(ids, userId))
                .thenReturn(List.of(scenario1, scenario2));

        ScenarioComparisonResponse response = scenarioService.compareScenarios(userId, ids);

        assertThat(response).isNotNull();
        assertThat(response.scenarios()).hasSize(2);
        assertThat(response.lowestMonthlyPaymentScenarioId()).isEqualTo(scenario1.getId());
        assertThat(response.lowestOverpaymentScenarioId()).isEqualTo(scenario2.getId());
    }

    @Test
    @DisplayName("Сравнение 3 сценариев: корректно выбирает победителей среди 3 вариантов")
    void compareScenarios_threeScenarios_success() {
        List<UUID> ids = List.of(scenario1.getId(), scenario2.getId(), scenario3.getId());
        when(scenarioRepository.findAllByIdInAndUserId(ids, userId))
                .thenReturn(List.of(scenario1, scenario2, scenario3));

        ScenarioComparisonResponse response = scenarioService.compareScenarios(userId, ids);

        assertThat(response.scenarios()).hasSize(3);
        assertThat(response.lowestMonthlyPaymentScenarioId()).isEqualTo(scenario1.getId());
        assertThat(response.lowestOverpaymentScenarioId()).isEqualTo(scenario2.getId());
    }

    @Test
    @DisplayName("Ошибка валидации: менее 2 сценариев выбрасывает BusinessException")
    void compareScenarios_lessThanTwo_throwsBusinessException() {
        List<UUID> ids = List.of(scenario1.getId());

        assertThatThrownBy(() -> scenarioService.compareScenarios(userId, ids))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("between 2 and 3 scenarios");
    }

    @Test
    @DisplayName("Ошибка валидации: более 3 сценариев выбрасывает BusinessException")
    void compareScenarios_moreThanThree_throwsBusinessException() {
        List<UUID> ids = List.of(scenario1.getId(), scenario2.getId(), scenario3.getId(), UUID.randomUUID());

        assertThatThrownBy(() -> scenarioService.compareScenarios(userId, ids))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("between 2 and 3 scenarios");
    }

    @Test
    @DisplayName("Ошибка: сценарий не найден или чужой выбрасывает ResourceNotFoundException")
    void compareScenarios_notFoundOrAlien_throwsResourceNotFound() {
        List<UUID> ids = List.of(scenario1.getId(), scenario2.getId());
        when(scenarioRepository.findAllByIdInAndUserId(ids, userId))
                .thenReturn(List.of(scenario1));

        assertThatThrownBy(() -> scenarioService.compareScenarios(userId, ids))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("One or more scenarios were not found");
    }

    @Test
    @DisplayName("Запрос экспорта PDF: публикует событие в очередь")
    void requestPdfExport_success() {
        when(scenarioRepository.findById(scenario1.getId()))
                .thenReturn(Optional.of(scenario1));

        scenarioService.requestPdfExport(userId, "user@example.com", scenario1.getId());

        verify(messageProducer).sendPdfExportEvent(any());
    }

    @Test
    @DisplayName("Запрос экспорта PDF: выбрасывает исключение если сценарий не найден")
    void requestPdfExport_notFound_throwsException() {
        when(scenarioRepository.findById(scenario1.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> scenarioService.requestPdfExport(userId, "user@example.com", scenario1.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
