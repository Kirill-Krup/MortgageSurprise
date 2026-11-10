package com.surprise.mortgagesurprise.dto.scenario;

import com.surprise.mortgagesurprise.model.entity.CalculationScenario;
import com.surprise.mortgagesurprise.model.enums.PaymentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ScenarioResponse(
        UUID id,
        UUID userId,
        String title,
        BigDecimal propertyPrice,
        BigDecimal downPayment,
        BigDecimal loanAmount,
        int termMonths,
        BigDecimal annualInterestRate,
        PaymentType paymentType,
        BigDecimal monthlyPayment,
        BigDecimal totalInterest,
        BigDecimal totalPayment,
        String mongoScheduleId,
        Instant createdAt,
        Instant updatedAt
) {
    public static ScenarioResponse from(CalculationScenario s) {
        return new ScenarioResponse(
                s.getId(),
                s.getUserId(),
                s.getTitle(),
                s.getPropertyPrice(),
                s.getDownPayment(),
                s.getLoanAmount(),
                s.getTermMonths(),
                s.getAnnualInterestRate(),
                s.getPaymentType(),
                s.getMonthlyPayment(),
                s.getTotalInterest(),
                s.getTotalPayment(),
                s.getMongoScheduleId(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
