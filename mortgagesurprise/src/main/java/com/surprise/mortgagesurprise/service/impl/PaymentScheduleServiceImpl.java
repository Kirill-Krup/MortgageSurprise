package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.repository.mongo.PaymentScheduleMongoRepository;
import com.surprise.mortgagesurprise.service.PaymentScheduleService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Single responsibility: Encapsulate PaymentScheduleDocument construction and persistence operations.
 */
@Service
public class PaymentScheduleServiceImpl implements PaymentScheduleService {

    private final PaymentScheduleMongoRepository scheduleMongoRepository;

    public PaymentScheduleServiceImpl(PaymentScheduleMongoRepository scheduleMongoRepository) {
        this.scheduleMongoRepository = scheduleMongoRepository;
    }

    @Override
    public PaymentScheduleDocument saveSchedule(CalculationResult result, String cacheHash) {
        PaymentScheduleDocument scheduleDoc = new PaymentScheduleDocument(
                UUID.randomUUID().toString(),
                cacheHash,
                result.propertyPrice(),
                result.downPayment(),
                result.loanAmount(),
                result.termMonths(),
                result.actualTermMonths(),
                result.annualInterestRate(),
                result.paymentType(),
                result.currency(),
                result.monthlyPayment(),
                result.totalInterest(),
                result.totalPayment(),
                result.schedule(),
                Instant.now()
        );
        return scheduleMongoRepository.save(scheduleDoc);
    }

    @Override
    public PaymentScheduleDocument saveSchedule(CalculationResult result) {
        return saveSchedule(result, null);
    }

    @Override
    public PaymentScheduleDocument getScheduleById(String scheduleId) {
        return scheduleMongoRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment schedule not found: " + scheduleId));
    }
}
