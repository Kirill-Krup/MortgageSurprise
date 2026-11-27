package com.surprise.mortgagesurprise.calculator.strategy;

import com.surprise.mortgagesurprise.calculator.CalculationContext;
import com.surprise.mortgagesurprise.calculator.EarlyRepaymentProcessor;
import com.surprise.mortgagesurprise.calculator.EarlyRepaymentResolution;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.dto.calculator.PaymentScheduleItem;
import com.surprise.mortgagesurprise.model.enums.Currency;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Base template for mortgage payment schedule calculation strategies.
 */
public abstract class AbstractPaymentCalculationStrategy<S> implements PaymentCalculationStrategy {

    public static final RoundingMode CURRENCY_ROUNDING = RoundingMode.HALF_UP;
    public static final int CURRENCY_SCALE = 2;
    protected static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);
    private static final int MAX_TERM_MULTIPLIER = 2;

    private final EarlyRepaymentProcessor earlyRepaymentProcessor;

    protected AbstractPaymentCalculationStrategy(EarlyRepaymentProcessor earlyRepaymentProcessor) {
        this.earlyRepaymentProcessor = earlyRepaymentProcessor;
    }

    @Override
    public CalculationResult calculate(CalculationContext context) {
        List<PaymentScheduleItem> schedule = new ArrayList<>();
        BigDecimal currentBalance = context.loanAmount();
        S state = createInitialState(context);

        BigDecimal totalInterest = BigDecimal.ZERO.setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        BigDecimal minPayment = null;
        BigDecimal maxPayment = null;
        BigDecimal firstMonthPayment = null;

        int month = 1;
        int maxLoopLimit = context.termMonths() * MAX_TERM_MULTIPLIER;

        while (currentBalance.compareTo(BigDecimal.ZERO) > 0 && month <= maxLoopLimit) {
            LocalDate paymentDate = context.startDate().plusMonths(month);
            BigDecimal openingBalance = currentBalance;
            BigDecimal interestPayment = openingBalance.multiply(context.monthlyRate(), MC)
                    .setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);

            BigDecimal principalPayment = calculatePrincipalPayment(
                    context, state, currentBalance, interestPayment, month);

            BigDecimal balanceAfterRegular = openingBalance.subtract(principalPayment)
                    .setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);

            EarlyRepaymentResolution earlyResolution = earlyRepaymentProcessor.resolve(
                    context.request().earlyRepayments(), month, balanceAfterRegular);
            BigDecimal earlyPayment = earlyResolution.actualAmount();
            BigDecimal closingBalance = balanceAfterRegular.subtract(earlyPayment)
                    .setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);

            BigDecimal totalMonthlyPayment = principalPayment.add(interestPayment).add(earlyPayment)
                    .setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);

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
            onMonthCompleted(context, state, closingBalance, earlyPayment, earlyResolution, month);
            month++;
        }

        int actualTermMonths = schedule.size();
        BigDecimal totalPayment = context.loanAmount().add(totalInterest)
                .setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);

        return new CalculationResult(
                context.propertyPrice(),
                context.downPayment(),
                context.loanAmount(),
                context.termMonths(),
                actualTermMonths,
                context.annualRate(),
                getSupportedPaymentType(),
                context.request().currency() != null ? context.request().currency() : Currency.RUB,
                firstMonthPayment != null ? firstMonthPayment : BigDecimal.ZERO,
                minPayment != null ? minPayment : BigDecimal.ZERO,
                maxPayment != null ? maxPayment : BigDecimal.ZERO,
                totalInterest,
                totalPayment,
                schedule
        );
    }

    protected abstract S createInitialState(CalculationContext context);

    protected abstract BigDecimal calculatePrincipalPayment(
            CalculationContext context,
            S state,
            BigDecimal currentBalance,
            BigDecimal interestPayment,
            int month
    );

    protected abstract void onMonthCompleted(
            CalculationContext context,
            S state,
            BigDecimal closingBalance,
            BigDecimal earlyPayment,
            EarlyRepaymentResolution earlyResolution,
            int month
    );
}
