package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.model.enums.RepaymentStrategy;

import java.math.BigDecimal;

/**
 * Encapsulates the resolved early repayment result for a specific payment month.
 */
public record EarlyRepaymentResolution(
        BigDecimal actualAmount,
        RepaymentStrategy strategy
) {}
