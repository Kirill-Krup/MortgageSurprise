package com.surprise.mortgagesurprise.calculator.strategy;

import com.surprise.mortgagesurprise.calculator.CalculationContext;
import com.surprise.mortgagesurprise.calculator.EarlyRepaymentProcessor;
import com.surprise.mortgagesurprise.calculator.EarlyRepaymentResolution;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.model.enums.RepaymentStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Single responsibility: Calculate differentiated mortgage payment schedules and statistics.
 */
@Component
public class DifferentiatedPaymentCalculationStrategy
        extends AbstractPaymentCalculationStrategy<DifferentiatedPaymentCalculationStrategy.DifferentiatedState> {

    public DifferentiatedPaymentCalculationStrategy(EarlyRepaymentProcessor earlyRepaymentProcessor) {
        super(earlyRepaymentProcessor);
    }

    @Override
    public PaymentType getSupportedPaymentType() {
        return PaymentType.DIFFERENTIATED;
    }

    @Override
    protected DifferentiatedState createInitialState(CalculationContext context) {
        int remainingMonths = context.termMonths();
        BigDecimal initialPrincipalPart = context.loanAmount().divide(
                BigDecimal.valueOf(remainingMonths), CURRENCY_SCALE, CURRENCY_ROUNDING);
        return new DifferentiatedState(remainingMonths, initialPrincipalPart);
    }

    @Override
    protected BigDecimal calculatePrincipalPayment(
            CalculationContext context,
            DifferentiatedState state,
            BigDecimal currentBalance,
            BigDecimal interestPayment,
            int month
    ) {
        BigDecimal principalPayment = state.currentPrincipalPart;
        if (principalPayment.compareTo(currentBalance) > 0 || month == context.termMonths()) {
            return currentBalance;
        }
        return principalPayment;
    }

    @Override
    protected void onMonthCompleted(
            CalculationContext context,
            DifferentiatedState state,
            BigDecimal closingBalance,
            BigDecimal earlyPayment,
            EarlyRepaymentResolution earlyResolution,
            int month
    ) {
        state.remainingMonths--;
        if (closingBalance.compareTo(BigDecimal.ZERO) > 0
                && earlyPayment.compareTo(BigDecimal.ZERO) > 0
                && earlyResolution.strategy() == RepaymentStrategy.REDUCE_PAYMENT
                && state.remainingMonths > 0) {
            state.currentPrincipalPart = closingBalance.divide(
                    BigDecimal.valueOf(state.remainingMonths), CURRENCY_SCALE, CURRENCY_ROUNDING);
        }
    }

    protected static final class DifferentiatedState {
        private int remainingMonths;
        private BigDecimal currentPrincipalPart;

        private DifferentiatedState(int remainingMonths, BigDecimal currentPrincipalPart) {
            this.remainingMonths = remainingMonths;
            this.currentPrincipalPart = currentPrincipalPart;
        }
    }
}
