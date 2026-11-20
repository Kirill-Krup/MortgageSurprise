package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.repository.mongo.PaymentScheduleMongoRepository;
import com.surprise.mortgagesurprise.testutil.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentScheduleServiceImpl Tests")
class PaymentScheduleServiceImplTest {

    @Mock
    private PaymentScheduleMongoRepository scheduleMongoRepository;

    @InjectMocks
    private PaymentScheduleServiceImpl paymentScheduleService;

    private CalculationResult calculationResult;

    @BeforeEach
    void setUp() {
        calculationResult = new CalculationResult(
                TestConstants.PROPERTY_PRICE,
                TestConstants.DOWN_PAYMENT,
                TestConstants.LOAN_AMOUNT,
                TestConstants.TERM_MONTHS,
                TestConstants.TERM_MONTHS,
                TestConstants.ANNUAL_RATE,
                PaymentType.ANNUITY,
                Currency.RUB,
                new BigDecimal("38600.00"),
                new BigDecimal("38600.00"),
                new BigDecimal("38600.00"),
                new BigDecimal("5200000.00"),
                new BigDecimal("9200000.00"),
                List.of()
        );
    }

    @Nested
    @DisplayName("Schedule Persistence")
    class SaveSchedule {

        @Test
        @DisplayName("Should map calculation result and persist document in MongoDB")
        void saveSchedule_shouldPersistDocument_whenCalculationResultProvided() {
            // Arrange
            String hash = TestConstants.CALCULATION_HASH;
            when(scheduleMongoRepository.save(any(PaymentScheduleDocument.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            PaymentScheduleDocument saved = paymentScheduleService.saveSchedule(calculationResult, hash);

            // Assert
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCalculationHash()).isEqualTo(hash);
            assertThat(saved.getLoanAmount()).isEqualByComparingTo(TestConstants.LOAN_AMOUNT);
            verify(scheduleMongoRepository, times(1)).save(any(PaymentScheduleDocument.class));
        }
    }

    @Nested
    @DisplayName("Schedule Retrieval")
    class GetScheduleById {

        @Test
        @DisplayName("Should return schedule document when found by ID")
        void getScheduleById_shouldReturnDocument_whenDocumentExists() {
            // Arrange
            PaymentScheduleDocument doc = new PaymentScheduleDocument();
            doc.setId(TestConstants.SCHEDULE_ID);
            when(scheduleMongoRepository.findById(TestConstants.SCHEDULE_ID)).thenReturn(Optional.of(doc));

            // Act
            PaymentScheduleDocument result = paymentScheduleService.getScheduleById(TestConstants.SCHEDULE_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TestConstants.SCHEDULE_ID);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when schedule not found by ID")
        void getScheduleById_shouldThrowResourceNotFoundException_whenDocumentNotFound() {
            // Arrange
            String missingId = "non-existent-id";
            when(scheduleMongoRepository.findById(missingId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentScheduleService.getScheduleById(missingId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Payment schedule not found");
        }
    }
}
