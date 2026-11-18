package com.surprise.mortgagesurprise.dto.admin;

import java.math.BigDecimal;

public record AdminStatsResponse(
        long totalUsers,
        long activeLoansCount,
        BigDecimal totalPortfolioAmount,
        long mortgageLoansCount,
        long microloansCount,
        BigDecimal mortgagePortfolioAmount,
        BigDecimal microloanPortfolioAmount,
        BigDecimal totalRepaidAmount,
        BigDecimal totalRemainingDebt,
        double repaymentProgressPercent,
        BigDecimal averageMortgageRate
) {
}
