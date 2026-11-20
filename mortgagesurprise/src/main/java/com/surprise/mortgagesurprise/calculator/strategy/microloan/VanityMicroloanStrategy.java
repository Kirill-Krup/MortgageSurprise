package com.surprise.mortgagesurprise.calculator.strategy.microloan;

import com.surprise.mortgagesurprise.calculator.factory.MicroloanCalculationContext;
import com.surprise.mortgagesurprise.dto.calculator.microloan.DailyScheduleItem;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResult;
import com.surprise.mortgagesurprise.model.enums.MicroloanType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class VanityMicroloanStrategy implements MicroloanCalculationStrategy {

    private static final RoundingMode CURRENCY_ROUNDING = RoundingMode.HALF_UP;
    private static final int SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Override
    public MicroloanType getSupportedType() {
        return MicroloanType.VANITY;
    }

    @Override
    public MicroloanCalculationResult calculate(MicroloanCalculationContext context) {
        BigDecimal loanAmount = context.loanAmount();
        int termDays = context.termDays();
        LocalDate startDate = context.startDate();

        BigDecimal dailyRateFraction = context.baseDailyRate().divide(ONE_HUNDRED, 16, RoundingMode.HALF_UP);
        BigDecimal dailyInterest = loanAmount.multiply(dailyRateFraction).setScale(SCALE, CURRENCY_ROUNDING);

        List<DailyScheduleItem> schedule = new ArrayList<>(termDays);
        BigDecimal accumulatedInterest = BigDecimal.ZERO;

        for (int day = 1; day <= termDays; day++) {
            accumulatedInterest = accumulatedInterest.add(dailyInterest);
            BigDecimal totalDue = loanAmount.add(accumulatedInterest);
            LocalDate dayDate = startDate.plusDays(day);

            schedule.add(new DailyScheduleItem(
                    day,
                    dayDate,
                    loanAmount,
                    dailyInterest,
                    accumulatedInterest,
                    totalDue
            ));
        }

        BigDecimal baseTotalInterest = accumulatedInterest;

        BigDecimal surchargeRate = context.vanitySurchargeRate() != null ? context.vanitySurchargeRate() : BigDecimal.ZERO;
        var vanitySurchargeAmount = context.vanitySurchargeRate();

        BigDecimal totalInterest = baseTotalInterest.add(vanitySurchargeAmount);
        BigDecimal totalRepayment = loanAmount.add(totalInterest);

        return new MicroloanCalculationResult(
                loanAmount,
                termDays,
                context.microloanType(),
                context.purpose(),
                context.baseDailyRate(),
                surchargeRate,
                vanitySurchargeAmount,
                context.baseDailyRate(),
                context.annualEquivalentRate(),
                totalInterest,
                totalRepayment,
                startDate,
                context.dueDate(),
                context.currency(),
                schedule
        );
    }
}
