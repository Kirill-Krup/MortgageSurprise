package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.dto.calculator.EarlyRepaymentRequest;
import com.surprise.mortgagesurprise.model.enums.EarlyRepaymentType;
import com.surprise.mortgagesurprise.model.enums.RepaymentStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Single responsibility: Process and resolve early repayments for a given month and available balance.
 */
@Component
public class EarlyRepaymentProcessor {

    public static final RoundingMode CURRENCY_ROUNDING = RoundingMode.HALF_UP;
    public static final int CURRENCY_SCALE = 2;

    public EarlyRepaymentResolution resolve(List<EarlyRepaymentRequest> earlyRepayments, int month, BigDecimal availableBalance) {
        if (earlyRepayments == null || earlyRepayments.isEmpty() || availableBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return new EarlyRepaymentResolution(BigDecimal.ZERO.setScale(CURRENCY_SCALE, CURRENCY_ROUNDING), RepaymentStrategy.REDUCE_PAYMENT);
        }

        BigDecimal requestedEarlyAmount = BigDecimal.ZERO;
        RepaymentStrategy finalStrategy = RepaymentStrategy.REDUCE_PAYMENT;

        for (EarlyRepaymentRequest er : earlyRepayments) {
            boolean applies = false;
            if (er.type() == EarlyRepaymentType.ONE_TIME) {
                applies = (er.startMonth() == month);
            } else if (er.type() == EarlyRepaymentType.RECURRING) {
                boolean afterOrAtStart = month >= er.startMonth();
                boolean beforeOrAtEnd = er.endMonth() == null || month <= er.endMonth();
                applies = afterOrAtStart && beforeOrAtEnd;
            }

            if (applies) {
                requestedEarlyAmount = requestedEarlyAmount.add(er.amount());
                finalStrategy = er.strategy();
            }
        }

        if (requestedEarlyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new EarlyRepaymentResolution(BigDecimal.ZERO.setScale(CURRENCY_SCALE, CURRENCY_ROUNDING), RepaymentStrategy.REDUCE_PAYMENT);
        }

        BigDecimal actualAmount = requestedEarlyAmount.min(availableBalance).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        return new EarlyRepaymentResolution(actualAmount, finalStrategy);
    }
}
