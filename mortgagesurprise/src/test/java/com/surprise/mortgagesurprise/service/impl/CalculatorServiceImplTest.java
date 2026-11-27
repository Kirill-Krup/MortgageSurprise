package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.calculator.CalculationCacheKeyGenerator;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.service.CalculationCacheService;
import com.surprise.mortgagesurprise.service.CalculationHistoryService;
import com.surprise.mortgagesurprise.service.MortgageCalculator;
import com.surprise.mortgagesurprise.service.PaymentScheduleService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CalculatorServiceImpl Tests")
class CalculatorServiceImplTest {

    @Mock
    private MortgageCalculator mortgageCalculator;

    @Mock
    private PaymentScheduleService paymentScheduleService;

    @Mock
    private CalculationHistoryService historyService;

    @Mock
    private CalculationCacheService cacheService;

    @Mock
    private CalculationCacheKeyGenerator cacheKeyGenerator;

    @InjectMocks
    private CalculatorServiceImpl calculatorService;

    private CalculationRequest request;
    private CalculationResult calculationResult;
    private PaymentScheduleDocument scheduleDoc;

    @BeforeEach
    void setUp() {
        request = new CalculationRequest(
                TestConstants.PROPERTY_PRICE,
                TestConstants.DOWN_PAYMENT,
                TestConstants.TERM_MONTHS,
                TestConstants.ANNUAL_RATE,
                PaymentType.ANNUITY,
                Currency.RUB,
                LocalDate.now(),
                List.of()
        );

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

        scheduleDoc = new PaymentScheduleDocument();
        scheduleDoc.setId(TestConstants.SCHEDULE_ID);
    }

    @Nested
    @DisplayName("Calculate and Persist Workflow")
    class CalculateAndPersistWorkflow {

        @Test
        @DisplayName("Should return cached calculation response when cache contains key")
        void calculateAndPersist_shouldReturnCachedResponse_whenCacheHit() {
            // Arrange
            String hash = TestConstants.CALCULATION_HASH;
            when(cacheKeyGenerator.generateKey(request)).thenReturn(hash);
            CalculationResponse cachedResponse = new CalculationResponse(
                    "cached-id",
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.DOWN_PAYMENT,
                    TestConstants.LOAN_AMOUNT,
                    TestConstants.TERM_MONTHS,
                    TestConstants.TERM_MONTHS,
                    TestConstants.ANNUAL_RATE,
                    "ANNUITY",
                    Currency.RUB,
                    new BigDecimal("38600.00"),
                    new BigDecimal("38600.00"),
                    new BigDecimal("38600.00"),
                    new BigDecimal("5200000.00"),
                    new BigDecimal("9200000.00"),
                    List.of()
            );
            when(cacheService.getFromCache(hash)).thenReturn(Optional.of(cachedResponse));

            // Act
            CalculationResponse response = calculatorService.calculateAndPersist(request, TestConstants.USER_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.scheduleId()).isEqualTo("cached-id");
            verifyNoInteractions(mortgageCalculator);
            verifyNoInteractions(paymentScheduleService);
            verifyNoInteractions(historyService);
        }

        @Test
        @DisplayName("Should calculate, persist schedule in MongoDB, record history, and save in cache when cache misses")
        void calculateAndPersist_shouldComputeSaveAndCache_whenCacheMiss() {
            // Arrange
            UUID userId = TestConstants.USER_ID;
            String hash = TestConstants.CALCULATION_HASH;
            when(cacheKeyGenerator.generateKey(request)).thenReturn(hash);
            when(cacheService.getFromCache(hash)).thenReturn(Optional.empty());
            when(mortgageCalculator.calculate(request)).thenReturn(calculationResult);
            when(paymentScheduleService.saveSchedule(calculationResult, hash)).thenReturn(scheduleDoc);

            // Act
            CalculationResponse response = calculatorService.calculateAndPersist(request, userId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.scheduleId()).isEqualTo(TestConstants.SCHEDULE_ID);
            assertThat(response.loanAmount()).isEqualByComparingTo(TestConstants.LOAN_AMOUNT);

            verify(paymentScheduleService, times(1)).saveSchedule(calculationResult, hash);
            verify(historyService, times(1)).recordHistory(userId, TestConstants.SCHEDULE_ID, calculationResult);
            verify(cacheService, times(1)).putInCache(eq(hash), any(CalculationResponse.class));
        }
    }
}
