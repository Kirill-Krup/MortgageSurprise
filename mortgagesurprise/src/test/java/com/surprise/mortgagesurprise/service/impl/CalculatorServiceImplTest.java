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
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                240,
                new BigDecimal("10.00"),
                PaymentType.ANNUITY,
                Currency.RUB,
                LocalDate.now(),
                List.of()
        );

        calculationResult = new CalculationResult(
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                new BigDecimal("4000000.00"),
                240,
                240,
                new BigDecimal("10.00"),
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
        scheduleDoc.setId("test-doc-id");
    }

    @Test
    @DisplayName("calculateAndPersist returns cached response when present")
    void testCachedResponse() {
        String hash = "hash-123";
        when(cacheKeyGenerator.generateKey(request)).thenReturn(hash);
        CalculationResponse cachedResponse = new CalculationResponse(
                "cached-id",
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                new BigDecimal("4000000.00"),
                240,
                240,
                new BigDecimal("10.00"),
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

        CalculationResponse response = calculatorService.calculateAndPersist(request, UUID.randomUUID());

        assertNotNull(response);
        assertEquals("cached-id", response.scheduleId());
        verifyNoInteractions(mortgageCalculator);
        verifyNoInteractions(paymentScheduleService);
    }

    @Test
    @DisplayName("calculateAndPersist computes, saves schedule and history, and caches response")
    void testComputeAndPersist() {
        UUID userId = UUID.randomUUID();
        String hash = "hash-456";
        when(cacheKeyGenerator.generateKey(request)).thenReturn(hash);
        when(cacheService.getFromCache(hash)).thenReturn(Optional.empty());
        when(mortgageCalculator.calculate(request)).thenReturn(calculationResult);
        when(paymentScheduleService.saveSchedule(calculationResult, hash)).thenReturn(scheduleDoc);

        CalculationResponse response = calculatorService.calculateAndPersist(request, userId);

        assertNotNull(response);
        assertEquals("test-doc-id", response.scheduleId());
        verify(paymentScheduleService).saveSchedule(calculationResult, hash);
        verify(historyService).recordHistory(userId, "test-doc-id", calculationResult);
        verify(cacheService).putInCache(eq(hash), any(CalculationResponse.class));
    }
}
