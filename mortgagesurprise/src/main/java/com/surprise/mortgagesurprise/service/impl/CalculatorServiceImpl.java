package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.model.document.CalculationHistoryDocument;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.repository.mongo.CalculationHistoryMongoRepository;
import com.surprise.mortgagesurprise.repository.mongo.PaymentScheduleMongoRepository;
import com.surprise.mortgagesurprise.service.CalculatorService;
import com.surprise.mortgagesurprise.service.MortgageCalculator;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class CalculatorServiceImpl implements CalculatorService {

    private final MortgageCalculator mortgageCalculator;
    private final PaymentScheduleMongoRepository scheduleMongoRepository;
    private final CalculationHistoryMongoRepository historyMongoRepository;

    public CalculatorServiceImpl(MortgageCalculator mortgageCalculator,
                                 PaymentScheduleMongoRepository scheduleMongoRepository,
                                 CalculationHistoryMongoRepository historyMongoRepository) {
        this.mortgageCalculator = mortgageCalculator;
        this.scheduleMongoRepository = scheduleMongoRepository;
        this.historyMongoRepository = historyMongoRepository;
    }

    @Override
    public CalculationResponse calculateAndPersist(CalculationRequest request, UUID userId) {
        CalculationResult result = mortgageCalculator.calculate(request);
        String hash = generateParamsHash(request);

        PaymentScheduleDocument scheduleDoc = new PaymentScheduleDocument(
                UUID.randomUUID().toString(),
                hash,
                result.propertyPrice(),
                result.downPayment(),
                result.loanAmount(),
                result.termMonths(),
                result.actualTermMonths(),
                result.annualInterestRate(),
                result.paymentType(),
                result.monthlyPayment(),
                result.totalInterest(),
                result.totalPayment(),
                result.schedule(),
                Instant.now()
        );
        PaymentScheduleDocument savedSchedule = scheduleMongoRepository.save(scheduleDoc);

        CalculationHistoryDocument historyDoc = new CalculationHistoryDocument(
                UUID.randomUUID().toString(),
                userId,
                savedSchedule.getId(),
                result.propertyPrice(),
                result.downPayment(),
                result.loanAmount(),
                result.termMonths(),
                result.annualInterestRate(),
                result.paymentType(),
                result.monthlyPayment(),
                result.totalInterest(),
                result.totalPayment(),
                Instant.now()
        );
        historyMongoRepository.save(historyDoc);

        return new CalculationResponse(
                savedSchedule.getId(),
                result.propertyPrice(),
                result.downPayment(),
                result.loanAmount(),
                result.termMonths(),
                result.actualTermMonths(),
                result.annualInterestRate(),
                result.paymentType().name(),
                result.monthlyPayment(),
                result.minMonthlyPayment(),
                result.maxMonthlyPayment(),
                result.totalInterest(),
                result.totalPayment(),
                result.schedule()
        );
    }

    @Override
    public PaymentScheduleDocument getScheduleById(String scheduleId) {
        return scheduleMongoRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment schedule not found: " + scheduleId));
    }

    private String generateParamsHash(CalculationRequest r) {
        try {
            String raw = r.propertyPrice() + ":" + r.downPayment() + ":" + r.termMonths() + ":"
                    + r.annualInterestRate() + ":" + r.paymentType() + ":" + (r.earlyRepayments() != null ? r.earlyRepayments().hashCode() : 0);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
