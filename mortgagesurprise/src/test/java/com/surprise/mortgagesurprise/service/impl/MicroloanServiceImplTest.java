package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.calculator.CalculationCacheKeyGenerator;
import com.surprise.mortgagesurprise.dto.calculator.microloan.DailyScheduleItem;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResponse;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResult;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.model.entity.MicroloanConfig;
import com.surprise.mortgagesurprise.model.entity.MicroloanRate;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.MicroloanType;
import com.surprise.mortgagesurprise.service.*;
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
@DisplayName("MicroloanServiceImpl Tests")
class MicroloanServiceImplTest {

    @Mock
    private MicroloanCalculator microloanCalculator;

    @Mock
    private PaymentScheduleService paymentScheduleService;

    @Mock
    private CalculationHistoryService historyService;

    @Mock
    private CalculationCacheKeyGenerator cacheKeyGenerator;

    @Mock
    private MicroloanCacheService cacheService;

    @Mock
    private MicroloanConfigService configService;

    @Mock
    private MicroloanRateService microloanRateService;

    @InjectMocks
    private MicroloanServiceImpl microloanService;

    private MicroloanCalculationRequest request;
    private MicroloanCalculationResult calculationResult;
    private PaymentScheduleDocument scheduleDoc;
    private MicroloanConfig microloanConfig;
    private MicroloanRate microloanRate;

    @BeforeEach
    void setUp() {
        request = new MicroloanCalculationRequest(
                TestConstants.MICROLOAN_AMOUNT,
                10,
                TestConstants.DEFAULT_RATE_NAME,
                MicroloanType.BASIC_NEEDS,
                "Продукты",
                null,
                LocalDate.of(2026, 12, 1),
                Currency.RUB
        );

        DailyScheduleItem scheduleItem = new DailyScheduleItem(
                1,
                LocalDate.of(2026, 12, 2),
                TestConstants.MICROLOAN_AMOUNT,
                new BigDecimal("80.00"),
                new BigDecimal("80.00"),
                new BigDecimal("10080.00")
        );

        calculationResult = new MicroloanCalculationResult(
                TestConstants.MICROLOAN_AMOUNT,
                10,
                MicroloanType.BASIC_NEEDS,
                "Продукты",
                TestConstants.BASE_DAILY_RATE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                TestConstants.BASE_DAILY_RATE,
                new BigDecimal("292.00"),
                new BigDecimal("800.00"),
                new BigDecimal("10800.00"),
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 11),
                Currency.RUB,
                List.of(scheduleItem)
        );

        scheduleDoc = new PaymentScheduleDocument();
        scheduleDoc.setId(TestConstants.SCHEDULE_ID);

        microloanConfig = new MicroloanConfig("DEFAULT", TestConstants.BANK_INTEREST);
        microloanRate = new MicroloanRate(TestConstants.DEFAULT_RATE_NAME, TestConstants.BASE_DAILY_RATE, 0, Integer.MAX_VALUE);
    }

    @Nested
    @DisplayName("Calculate and Persist Workflow")
    class CalculateAndPersistWorkflow {

        @Test
        @DisplayName("Should return cached microloan response when Redis cache contains key")
        void calculateAndPersist_shouldReturnCachedResponse_whenCacheHit() {
            // Arrange
            String hash = TestConstants.CALCULATION_HASH;
            when(configService.getConfig()).thenReturn(microloanConfig);
            when(microloanRateService.findByName(TestConstants.DEFAULT_RATE_NAME)).thenReturn(microloanRate);
            when(cacheKeyGenerator.generateKey(request, TestConstants.BASE_DAILY_RATE, TestConstants.BANK_INTEREST))
                    .thenReturn(hash);

            MicroloanCalculationResponse cachedResponse = new MicroloanCalculationResponse(
                    "cached-micro-id",
                    TestConstants.MICROLOAN_AMOUNT,
                    10,
                    MicroloanType.BASIC_NEEDS,
                    "Продукты",
                    TestConstants.BASE_DAILY_RATE,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    TestConstants.BASE_DAILY_RATE,
                    new BigDecimal("292.00"),
                    new BigDecimal("800.00"),
                    new BigDecimal("10800.00"),
                    LocalDate.of(2026, 12, 1),
                    LocalDate.of(2026, 12, 11),
                    Currency.RUB,
                    List.of()
            );
            when(cacheService.getFromCache(hash)).thenReturn(Optional.of(cachedResponse));

            // Act
            MicroloanCalculationResponse response = microloanService.calculateAndPersist(request, TestConstants.USER_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.scheduleId()).isEqualTo("cached-micro-id");
            verifyNoInteractions(microloanCalculator);
            verifyNoInteractions(paymentScheduleService);
            verifyNoInteractions(historyService);
        }

        @Test
        @DisplayName("Should orchestrate calculation, save schedule, record history and cache result when cache misses")
        void calculateAndPersist_shouldOrchestrateCalculationAndPersist_whenCacheMiss() {
            // Arrange
            UUID userId = TestConstants.USER_ID;
            String hash = TestConstants.CALCULATION_HASH;
            when(configService.getConfig()).thenReturn(microloanConfig);
            when(microloanRateService.findByName(TestConstants.DEFAULT_RATE_NAME)).thenReturn(microloanRate);
            when(cacheKeyGenerator.generateKey(request, TestConstants.BASE_DAILY_RATE, TestConstants.BANK_INTEREST))
                    .thenReturn(hash);
            when(cacheService.getFromCache(hash)).thenReturn(Optional.empty());
            when(microloanCalculator.calculate(request)).thenReturn(calculationResult);
            when(paymentScheduleService.saveSchedule(calculationResult, hash)).thenReturn(scheduleDoc);

            // Act
            MicroloanCalculationResponse response = microloanService.calculateAndPersist(request, userId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.scheduleId()).isEqualTo(TestConstants.SCHEDULE_ID);
            assertThat(response.loanAmount()).isEqualByComparingTo(TestConstants.MICROLOAN_AMOUNT);
            assertThat(response.totalRepayment()).isEqualByComparingTo("10800.00");

            verify(paymentScheduleService, times(1)).saveSchedule(calculationResult, hash);
            verify(historyService, times(1)).recordHistory(userId, TestConstants.SCHEDULE_ID, calculationResult);
            verify(cacheService, times(1)).putInCache(eq(hash), any(MicroloanCalculationResponse.class));
        }
    }
}
