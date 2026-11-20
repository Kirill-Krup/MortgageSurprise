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
public class BasicNeedsMicroloanStrategy implements MicroloanCalculationStrategy {

    private static final RoundingMode CURRENCY_ROUNDING = RoundingMode.HALF_UP;
    private static final int SCALE = 2;

    @Override
    public MicroloanType getSupportedType() {
        return MicroloanType.BASIC_NEEDS;
    }

    @Override
    public MicroloanCalculationResult calculate(MicroloanCalculationContext context) {
        BigDecimal loanAmount = context.loanAmount();
        int termDays = context.termDays();
        BigDecimal dailyRateFraction = context.effectiveDailyRateFraction();
        LocalDate startDate = context.startDate();

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

        BigDecimal totalInterest = accumulatedInterest;
        BigDecimal totalRepayment = loanAmount.add(totalInterest);

        return new MicroloanCalculationResult(
                loanAmount,
                termDays,
                context.microloanType(),
                context.purpose(),
                context.baseDailyRate(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                context.effectiveDailyRate(),
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
