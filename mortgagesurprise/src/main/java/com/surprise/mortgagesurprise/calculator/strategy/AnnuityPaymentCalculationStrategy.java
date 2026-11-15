package com.surprise.mortgagesurprise.calculator.strategy;

import com.surprise.mortgagesurprise.calculator.CalculationContext;
import com.surprise.mortgagesurprise.calculator.EarlyRepaymentProcessor;
import com.surprise.mortgagesurprise.calculator.EarlyRepaymentResolution;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.dto.calculator.PaymentScheduleItem;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.model.enums.RepaymentStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Single responsibility: Calculate annuity mortgage payment schedules and statistics.
 */
@Component
public class AnnuityPaymentCalculationStrategy implements PaymentCalculationStrategy {

    public static final RoundingMode CURRENCY_ROUNDING = RoundingMode.HALF_UP;
    public static final int CURRENCY_SCALE = 2;
    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    private final EarlyRepaymentProcessor earlyRepaymentProcessor;

    public AnnuityPaymentCalculationStrategy(EarlyRepaymentProcessor earlyRepaymentProcessor) {
        this.earlyRepaymentProcessor = earlyRepaymentProcessor;
    }

    @Override
    public PaymentType getSupportedPaymentType() {
        return PaymentType.ANNUITY;
    }

    @Override
    public CalculationResult calculate(CalculationContext context) {
        List<PaymentScheduleItem> schedule = new ArrayList<>();
        BigDecimal currentBalance = context.loanAmount();
        BigDecimal currentAnnuityPayment = computeAnnuityPayment(currentBalance, context.monthlyRate(), context.termMonths());

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

            BigDecimal principalPayment;
            BigDecimal plannedPayment = currentAnnuityPayment;

            if (currentBalance.add(interestPayment).compareTo(plannedPayment) <= 0) {
                principalPayment = currentBalance;
            } else {
                principalPayment = plannedPayment.subtract(interestPayment);
                if (principalPayment.compareTo(currentBalance) > 0) {
                    principalPayment = currentBalance;
                }
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

            if (closingBalance.compareTo(BigDecimal.ZERO) > 0 && earlyPayment.compareTo(BigDecimal.ZERO) > 0) {
                if (earlyResolution.strategy() == RepaymentStrategy.REDUCE_PAYMENT) {
                    int remainingMonths = context.termMonths() - month;
                    if (remainingMonths > 0) {
                        currentAnnuityPayment = computeAnnuityPayment(closingBalance, context.monthlyRate(), remainingMonths);
                    }
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
                PaymentType.ANNUITY,
                context.request().currency() != null ? context.request().currency() : Currency.RUB,
                firstMonthPayment != null ? firstMonthPayment : BigDecimal.ZERO,
                minPayment != null ? minPayment : BigDecimal.ZERO,
                maxPayment != null ? maxPayment : BigDecimal.ZERO,
                totalInterest,
                totalPayment,
                schedule
        );
    }

    public BigDecimal computeAnnuityPayment(BigDecimal principal, BigDecimal monthlyRate, int months) {
        if (months <= 0 || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        }
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), CURRENCY_SCALE, CURRENCY_ROUNDING);
        }

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate, MC);
        BigDecimal pow = onePlusR.pow(months, MC);
        BigDecimal numerator = monthlyRate.multiply(pow, MC);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE, MC);
        BigDecimal coefficient = numerator.divide(denominator, MC);

        return principal.multiply(coefficient, MC).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
    }
}
