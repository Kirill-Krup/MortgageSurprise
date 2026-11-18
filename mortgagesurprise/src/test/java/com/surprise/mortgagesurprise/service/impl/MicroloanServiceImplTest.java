package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.calculator.CalculationCacheKeyGenerator;
import com.surprise.mortgagesurprise.dto.calculator.microloan.DailyScheduleItem;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResponse;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResult;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.model.entity.MicroloanConfig;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.MicroloanType;
import com.surprise.mortgagesurprise.service.*;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

    @InjectMocks
    private MicroloanServiceImpl microloanService;

    @Test
    @DisplayName("Should orchestrate microloan calculation, persist schedule, record history and put in Redis cache")
    void shouldCalculateAndPersistMicroloan() {
        UUID userId = UUID.randomUUID();
        MicroloanCalculationRequest request = new MicroloanCalculationRequest(
                new BigDecimal("15000.00"),
                14,
                "Test rate",
                MicroloanType.BASIC_NEEDS,
                "Непредвиденные расходы",
                null,
                LocalDate.now(),
                Currency.RUB
        );

        when(configService.getConfig()).thenReturn(new MicroloanConfig(
                "DEFAULT",
                new BigDecimal("2000")
        ));

        when(cacheKeyGenerator.generateKey(request, new BigDecimal("0.8000"), new BigDecimal("5.0000")))
                .thenReturn("sample-hash-123");
        when(cacheService.getFromCache("sample-hash-123")).thenReturn(Optional.empty());

        MicroloanCalculationResult calcResult = new MicroloanCalculationResult(
                new BigDecimal("15000.00"),
                14,
                MicroloanType.BASIC_NEEDS,
                "Непредвиденные расходы",
                new BigDecimal("0.8000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("0.8000"),
                new BigDecimal("292.00"),
                new BigDecimal("1680.00"),
                new BigDecimal("16680.00"),
                LocalDate.now(),
                LocalDate.now().plusDays(14),
                Currency.RUB,
                List.of(new DailyScheduleItem(1, LocalDate.now().plusDays(1), new BigDecimal("15000.00"), new BigDecimal("120.00"), new BigDecimal("120.00"), new BigDecimal("15120.00")))
        );
        when(microloanCalculator.calculate(request)).thenReturn(calcResult);

        PaymentScheduleDocument mockDoc = new PaymentScheduleDocument();
        mockDoc.setId("doc-uuid-999");
        when(paymentScheduleService.saveSchedule(eq(calcResult), eq("sample-hash-123"))).thenReturn(mockDoc);

        MicroloanCalculationResponse response = microloanService.calculateAndPersist(request, userId);

        assertThat(response).isNotNull();
        assertThat(response.scheduleId()).isEqualTo("doc-uuid-999");
        assertThat(response.loanAmount()).isEqualByComparingTo("15000.00");
        assertThat(response.totalInterest()).isEqualByComparingTo("1680.00");
        assertThat(response.totalRepayment()).isEqualByComparingTo("16680.00");
        assertThat(response.microloanType()).isEqualTo(MicroloanType.BASIC_NEEDS);

        verify(paymentScheduleService).saveSchedule(eq(calcResult), eq("sample-hash-123"));
        verify(historyService).recordHistory(eq(userId), eq("doc-uuid-999"), eq(calcResult));
        verify(cacheService).putInCache(eq("sample-hash-123"), any(MicroloanCalculationResponse.class));
    }
}
