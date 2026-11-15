package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.repository.mongo.PaymentScheduleMongoRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentScheduleServiceImplTest {

    @Mock
    private PaymentScheduleMongoRepository scheduleMongoRepository;

    @InjectMocks
    private PaymentScheduleServiceImpl paymentScheduleService;

    private CalculationResult calculationResult;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    @DisplayName("saveSchedule maps result and persists document in MongoDB")
    void testSaveSchedule() {
        when(scheduleMongoRepository.save(any(PaymentScheduleDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentScheduleDocument saved = paymentScheduleService.saveSchedule(calculationResult, "test-hash");

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("test-hash", saved.getCalculationHash());
        assertEquals(calculationResult.loanAmount(), saved.getLoanAmount());
        verify(scheduleMongoRepository, times(1)).save(any(PaymentScheduleDocument.class));
    }

    @Test
    @DisplayName("getScheduleById returns document when found")
    void testGetScheduleByIdFound() {
        PaymentScheduleDocument doc = new PaymentScheduleDocument();
        when(scheduleMongoRepository.findById("doc-123")).thenReturn(Optional.of(doc));

        PaymentScheduleDocument result = paymentScheduleService.getScheduleById("doc-123");
        assertNotNull(result);
        assertEquals(doc, result);
    }

    @Test
    @DisplayName("getScheduleById throws ResourceNotFoundException when document missing")
    void testGetScheduleByIdNotFound() {
        when(scheduleMongoRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentScheduleService.getScheduleById("missing"));
    }
}
