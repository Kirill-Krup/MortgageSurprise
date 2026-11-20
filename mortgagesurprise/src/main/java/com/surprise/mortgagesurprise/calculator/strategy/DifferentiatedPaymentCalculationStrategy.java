package com.surprise.mortgagesurprise.calculator.strategy;

import com.surprise.mortgagesurprise.calculator.CalculationContext;
import com.surprise.mortgagesurprise.calculator.EarlyRepaymentProcessor;
import com.surprise.mortgagesurprise.calculator.EarlyRepaymentResolution;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.dto.calculator.PaymentScheduleItem;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.model.enums.RepaymentStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Single responsibility: Calculate differentiated mortgage payment schedules and statistics.
 */
@Component
@RequiredArgsConstructor
public class DifferentiatedPaymentCalculationStrategy implements PaymentCalculationStrategy {

    public static final RoundingMode CURRENCY_ROUNDING = RoundingMode.HALF_UP;
    public static final int CURRENCY_SCALE = 2;
    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    private final EarlyRepaymentProcessor earlyRepaymentProcessor;

    @Override
    public PaymentType getSupportedPaymentType() {
        return PaymentType.DIFFERENTIATED;
    }

    @Override
    public CalculationResult calculate(CalculationContext context) {
        List<PaymentScheduleItem> schedule = new ArrayList<>();
        BigDecimal currentBalance = context.loanAmount();
        int remainingMonths = context.termMonths();
        BigDecimal currentPrincipalPart = currentBalance.divide(
                BigDecimal.valueOf(remainingMonths), CURRENCY_SCALE, CURRENCY_ROUNDING);

        BigDecimal totalInterest = BigDecimal.ZERO.setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        BigDecimal minPayment = null;
        BigDecimal maxPayment = null;
        BigDecimal firstMonthPayment = null;

        int month = 1;
        int maxLoopLimit = context.termMonths() * 2;

        while (currentBalance.compareTo(BigDecimal.ZERO) > 0 && month <= maxLoopLimit) {
            LocalDate paymentDate = context.startDate().plusMonths(month);
            BigDecimal openingBalance = currentBalance;

            BigDecimal interestPayment = openingBalance.multiply(context.monthlyRate(), MC).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);

            BigDecimal principalPayment = currentPrincipalPart;
            if (principalPayment.compareTo(currentBalance) > 0 || month == context.termMonths()) {
                principalPayment = currentBalance;
            }

            BigDecimal balanceAfterRegular = openingBalance.subtract(principalPayment).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);

            EarlyRepaymentResolution earlyResolution = earlyRepaymentProcessor.resolve(
                    context.request().earlyRepayments(), month, balanceAfterRegular);
            BigDecimal earlyPayment = earlyResolution.actualAmount();
            BigDecimal closingBalance = balanceAfterRegular.subtract(earlyPayment).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);

            BigDecimal totalMonthlyPayment = principalPayment.add(interestPayment).add(earlyPayment).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);

            schedule.add(new PaymentScheduleItem(
                    month,
                    paymentDate,
                    openingBalance,
                    principalPayment,
                    interestPayment,
                    earlyPayment,
                    totalMonthlyPayment,
                    closingBalance
            ));

            totalInterest = totalInterest.add(interestPayment);

            if (firstMonthPayment == null) {
                firstMonthPayment = totalMonthlyPayment;
            }
            if (minPayment == null || totalMonthlyPayment.compareTo(minPayment) < 0) {
                minPayment = totalMonthlyPayment;
            }
            if (maxPayment == null || totalMonthlyPayment.compareTo(maxPayment) > 0) {
                maxPayment = totalMonthlyPayment;
            }

            currentBalance = closingBalance;
            remainingMonths--;

            if (closingBalance.compareTo(BigDecimal.ZERO) > 0 && earlyPayment.compareTo(BigDecimal.ZERO) > 0) {
                if (earlyResolution.strategy() == RepaymentStrategy.REDUCE_PAYMENT && remainingMonths > 0) {
                    currentPrincipalPart = closingBalance.divide(
                            BigDecimal.valueOf(remainingMonths), CURRENCY_SCALE, CURRENCY_ROUNDING);
                }
            }

            month++;
        }

        int actualTermMonths = schedule.size();
        BigDecimal totalPayment = context.loanAmount().add(totalInterest).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);

        return new CalculationResult(
                context.propertyPrice(),
                context.downPayment(),
                context.loanAmount(),
                context.termMonths(),
                actualTermMonths,
                context.annualRate(),
                PaymentType.DIFFERENTIATED,
                context.request().currency() != null ? context.request().currency() : Currency.RUB,
                firstMonthPayment != null ? firstMonthPayment : BigDecimal.ZERO,
                minPayment != null ? minPayment : BigDecimal.ZERO,
                maxPayment != null ? maxPayment : BigDecimal.ZERO,
                totalInterest,
                totalPayment,
                schedule
        );
    }
}
