package com.surprise.mortgagesurprise.calculator.strategy;

import com.surprise.mortgagesurprise.calculator.CalculationContext;
import com.surprise.mortgagesurprise.calculator.EarlyRepaymentProcessor;
import com.surprise.mortgagesurprise.calculator.EarlyRepaymentResolution;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.model.enums.RepaymentStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Single responsibility: Calculate annuity mortgage payment schedules and statistics.
 */
@Component
public class AnnuityPaymentCalculationStrategy
        extends AbstractPaymentCalculationStrategy<AnnuityPaymentCalculationStrategy.AnnuityState> {

    public AnnuityPaymentCalculationStrategy(EarlyRepaymentProcessor earlyRepaymentProcessor) {
        super(earlyRepaymentProcessor);
    }

    @Override
    public PaymentType getSupportedPaymentType() {
        return PaymentType.ANNUITY;
    }

    @Override
    protected AnnuityState createInitialState(CalculationContext context) {
        BigDecimal initialAnnuity = computeAnnuityPayment(
                context.loanAmount(), context.monthlyRate(), context.termMonths());
        return new AnnuityState(initialAnnuity);
    }

    @Override
    protected BigDecimal calculatePrincipalPayment(
            CalculationContext context,
            AnnuityState state,
            BigDecimal currentBalance,
            BigDecimal interestPayment,
            int month
    ) {
        BigDecimal plannedPayment = state.currentAnnuityPayment;
        if (currentBalance.add(interestPayment).compareTo(plannedPayment) <= 0) {
            return currentBalance;
        }
        BigDecimal principalPayment = plannedPayment.subtract(interestPayment);
        return principalPayment.compareTo(currentBalance) > 0 ? currentBalance : principalPayment;
    }

    @Override
    protected void onMonthCompleted(
            CalculationContext context,
            AnnuityState state,
            BigDecimal closingBalance,
            BigDecimal earlyPayment,
            EarlyRepaymentResolution earlyResolution,
            int month
    ) {
        if (closingBalance.compareTo(BigDecimal.ZERO) > 0
                && earlyPayment.compareTo(BigDecimal.ZERO) > 0
                && earlyResolution.strategy() == RepaymentStrategy.REDUCE_PAYMENT) {
            int remainingMonths = context.termMonths() - month;
            if (remainingMonths > 0) {
                state.currentAnnuityPayment = computeAnnuityPayment(
                        closingBalance, context.monthlyRate(), remainingMonths);
            }
        }
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

    protected static final class AnnuityState {
        private BigDecimal currentAnnuityPayment;

        private AnnuityState(BigDecimal currentAnnuityPayment) {
            this.currentAnnuityPayment = currentAnnuityPayment;
        }
    }
}
