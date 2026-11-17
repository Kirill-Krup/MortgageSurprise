package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResult;
import com.surprise.mortgagesurprise.model.document.CalculationHistoryDocument;
import com.surprise.mortgagesurprise.repository.mongo.CalculationHistoryMongoRepository;
import com.surprise.mortgagesurprise.service.CalculationHistoryService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Single responsibility: Encapsulate CalculationHistoryDocument creation and persistence.
 */
@Service
public class CalculationHistoryServiceImpl implements CalculationHistoryService {

    private final CalculationHistoryMongoRepository historyMongoRepository;

    public CalculationHistoryServiceImpl(CalculationHistoryMongoRepository historyMongoRepository) {
        this.historyMongoRepository = historyMongoRepository;
    }

    @Override
    public void recordHistory(UUID userId, String scheduleId, CalculationResult result) {
        if (userId == null) {
            return;
        }

        CalculationHistoryDocument historyDoc = new CalculationHistoryDocument(
                UUID.randomUUID().toString(),
                userId,
                scheduleId,
                result.propertyPrice(),
                result.downPayment(),
                result.loanAmount(),
                result.termMonths(),
                result.annualInterestRate(),
                result.paymentType(),
                result.currency(),
                result.monthlyPayment(),
                result.totalInterest(),
                result.totalPayment(),
                Instant.now()
        );
        historyMongoRepository.save(historyDoc);
    }

    @Override
    public void recordHistory(UUID userId, String scheduleId, MicroloanCalculationResult result) {
        if (userId == null) {
            return;
        }

        CalculationHistoryDocument historyDoc = new CalculationHistoryDocument(
                UUID.randomUUID().toString(),
                userId,
                scheduleId,
                result.loanAmount(),
                result.termDays(),
                result.dailyInterestRate(),
                result.annualEquivalentRate(),
                result.currency(),
                result.totalInterest(),
                result.totalRepayment(),
                Instant.now()
        );
        historyMongoRepository.save(historyDoc);
    }
}
