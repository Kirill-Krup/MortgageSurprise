package com.surprise.mortgagesurprise.calculator.factory;

import com.surprise.mortgagesurprise.calculator.CalculationContext;
import com.surprise.mortgagesurprise.calculator.CalculationRequestValidator;
import com.surprise.mortgagesurprise.calculator.util.MicroloanPurposeClassifier;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.model.enums.MicroloanType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;


@Component
@RequiredArgsConstructor
public class CalculationContextFactory {

    public static final RoundingMode CURRENCY_ROUNDING = RoundingMode.HALF_UP;
    public static final int CURRENCY_SCALE = 2;
    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);
    private static final BigDecimal TWELVE_HUNDRED = new BigDecimal("1200");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    private final CalculationRequestValidator mortgageValidator;

    public CalculationContext createMortgageContext(CalculationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Mortgage calculation request cannot be null");
        }
        mortgageValidator.validate(request);

        BigDecimal propertyPrice = request.propertyPrice().setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        BigDecimal downPayment = request.downPayment().setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        BigDecimal loanAmount = propertyPrice.subtract(downPayment).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        int termMonths = request.termMonths();
        BigDecimal annualRate = request.annualInterestRate();
        LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now();
        BigDecimal monthlyRate = annualRate.divide(TWELVE_HUNDRED, MC);

        return new CalculationContext(
                request,
                propertyPrice,
                downPayment,
                loanAmount,
                termMonths,
                annualRate,
                monthlyRate,
                startDate
        );
    }

    public MicroloanCalculationContext createMicroloanContext(MicroloanCalculationRequest request,
                                                             BigDecimal configuredBaseRate,
                                                             BigDecimal bankInterest) {
        if (request == null) {
            throw new IllegalArgumentException("Microloan calculation request cannot be null");
        }
        validateMicroloanRequest(request);

        BigDecimal loanAmount = request.amount().setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        int termDays = request.termDays();
        String purpose = request.purpose() != null ? request.purpose().trim() : "";

        MicroloanType type = MicroloanPurposeClassifier.classify(purpose);
        if (request.type() == MicroloanType.VANITY) {
            type = MicroloanType.VANITY;
        }

        BigDecimal baseRate = (configuredBaseRate != null ? configuredBaseRate : new BigDecimal("0.8000"))
                .setScale(4, CURRENCY_ROUNDING);
        BigDecimal vanitySurcharge = (bankInterest != null ? bankInterest :
                new BigDecimal("2000")).setScale(4, CURRENCY_ROUNDING);

        BigDecimal dailyRateFraction = baseRate.divide(ONE_HUNDRED, MC);
        BigDecimal annualEquivalentRate = baseRate.multiply(DAYS_IN_YEAR).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now();
        LocalDate dueDate = startDate.plusDays(termDays);

        return new MicroloanCalculationContext(
                request,
                loanAmount,
                termDays,
                type,
                purpose,
                baseRate,
                vanitySurcharge,
                baseRate,
                dailyRateFraction,
                annualEquivalentRate,
                startDate,
                dueDate,
                request.currency()
        );
    }

    public MicroloanCalculationContext createMicroloanContext(MicroloanCalculationRequest request) {
        BigDecimal base = request.dailyInterestRate() != null ? request.dailyInterestRate() : new BigDecimal("0.8000");
        return createMicroloanContext(request, base, new BigDecimal("5.0000"));
    }

    private void validateMicroloanRequest(MicroloanCalculationRequest request) {
        if (request.amount() == null || request.amount().compareTo(new BigDecimal("1000")) < 0) {
            throw new IllegalArgumentException("Microloan amount must be at least 1,000");
        }
        if (request.amount().compareTo(new BigDecimal("100000")) > 0) {
            throw new IllegalArgumentException("Microloan amount cannot exceed 100,000");
        }
        if (request.termDays() == null || request.termDays() < 7 || request.termDays() > 30) {
            throw new IllegalArgumentException("Microloan term must be between 7 and 30 days");
        }
    }
}
