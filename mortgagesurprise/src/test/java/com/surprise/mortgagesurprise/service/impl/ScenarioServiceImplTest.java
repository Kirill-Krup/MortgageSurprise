package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.scenario.ScenarioComparisonResponse;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.messaging.event.PdfExportEvent;
import com.surprise.mortgagesurprise.messaging.sender.RabbitMqEventSender;
import com.surprise.mortgagesurprise.model.entity.CalculationScenario;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.repository.jpa.CalculationScenarioRepository;
import com.surprise.mortgagesurprise.service.MortgageCalculator;
import com.surprise.mortgagesurprise.service.PaymentScheduleService;
import com.surprise.mortgagesurprise.testutil.TestConstants;
import com.surprise.mortgagesurprise.util.ScenarioComparator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("ScenarioServiceImpl Tests")
class ScenarioServiceImplTest {

    @Mock
    private CalculationScenarioRepository scenarioRepository;

    @Mock
    private MortgageCalculator mortgageCalculator;

    @Mock
    private PaymentScheduleService paymentScheduleService;

    @Spy
    private ScenarioComparator scenarioComparator = new ScenarioComparator();

    @Mock
    private RabbitMqEventSender<PdfExportEvent> messageProducer;

    @InjectMocks
    private ScenarioServiceImpl scenarioService;

    private UUID userId;
    private CalculationScenario scenario1;
    private CalculationScenario scenario2;
    private CalculationScenario scenario3;

    @BeforeEach
    void setUp() {
        userId = TestConstants.USER_ID;

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

    @Nested
    @DisplayName("Compare Scenarios")
    class CompareScenarios {

        @Test
        @DisplayName("Should accurately determine winners when comparing two scenarios")
        void compareScenarios_shouldIdentifyWinners_whenTwoScenariosProvided() {
            // Arrange
            List<UUID> ids = List.of(scenario1.getId(), scenario2.getId());
            when(scenarioRepository.findAllByIdInAndUserId(ids, userId))
                    .thenReturn(List.of(scenario1, scenario2));

            // Act
            ScenarioComparisonResponse response = scenarioService.compareScenarios(userId, ids);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.scenarios()).hasSize(2);
            assertThat(response.lowestMonthlyPaymentScenarioId()).isEqualTo(scenario1.getId());
            assertThat(response.lowestOverpaymentScenarioId()).isEqualTo(scenario2.getId());
        }

        @Test
        @DisplayName("Should accurately select winners when comparing three scenarios")
        void compareScenarios_shouldIdentifyWinners_whenThreeScenariosProvided() {
            // Arrange
            List<UUID> ids = List.of(scenario1.getId(), scenario2.getId(), scenario3.getId());
            when(scenarioRepository.findAllByIdInAndUserId(ids, userId))
                    .thenReturn(List.of(scenario1, scenario2, scenario3));

            // Act
            ScenarioComparisonResponse response = scenarioService.compareScenarios(userId, ids);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.scenarios()).hasSize(3);
            assertThat(response.lowestMonthlyPaymentScenarioId()).isEqualTo(scenario1.getId());
            assertThat(response.lowestOverpaymentScenarioId()).isEqualTo(scenario2.getId());
        }

        @Test
        @DisplayName("Should throw BusinessException when fewer than 2 scenarios provided")
        void compareScenarios_shouldThrowBusinessException_whenFewerThanTwoScenariosProvided() {
            // Arrange
            List<UUID> ids = List.of(scenario1.getId());

            // Act & Assert
            assertThatThrownBy(() -> scenarioService.compareScenarios(userId, ids))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("between 2 and 3 scenarios");
        }

        @Test
        @DisplayName("Should throw BusinessException when more than 3 scenarios provided")
        void compareScenarios_shouldThrowBusinessException_whenMoreThanThreeScenariosProvided() {
            // Arrange
            List<UUID> ids = List.of(scenario1.getId(), scenario2.getId(), scenario3.getId(), UUID.randomUUID());

            // Act & Assert
            assertThatThrownBy(() -> scenarioService.compareScenarios(userId, ids))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("between 2 and 3 scenarios");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when scenario is not found or belongs to another user")
        void compareScenarios_shouldThrowResourceNotFoundException_whenScenarioNotFoundOrAlien() {
            // Arrange
            List<UUID> ids = List.of(scenario1.getId(), scenario2.getId());
            when(scenarioRepository.findAllByIdInAndUserId(ids, userId))
                    .thenReturn(List.of(scenario1));

            // Act & Assert
            assertThatThrownBy(() -> scenarioService.compareScenarios(userId, ids))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("One or more scenarios were not found");
        }
    }

    @Nested
    @DisplayName("PDF Export Request")
    class PdfExportRequest {

        @Test
        @DisplayName("Should publish PDF export event to message broker when scenario exists")
        void requestPdfExport_shouldPublishPdfExportEvent_whenScenarioExists() {
            // Arrange
            when(scenarioRepository.findById(scenario1.getId()))
                    .thenReturn(Optional.of(scenario1));

            // Act
            scenarioService.requestPdfExport(userId, TestConstants.USER_EMAIL, scenario1.getId());

            // Assert
            verify(messageProducer).send(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when scenario to export is not found")
        void requestPdfExport_shouldThrowResourceNotFoundException_whenScenarioNotFound() {
            // Arrange
            UUID scenarioId = scenario1.getId();
            when(scenarioRepository.findById(scenarioId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> scenarioService.requestPdfExport(userId, TestConstants.USER_EMAIL, scenarioId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
