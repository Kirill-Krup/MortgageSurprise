package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.dto.calculator.EarlyRepaymentRequest;
import com.surprise.mortgagesurprise.dto.calculator.PaymentScheduleItem;
import com.surprise.mortgagesurprise.model.enums.EarlyRepaymentType;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.model.enums.RepaymentStrategy;
import com.surprise.mortgagesurprise.service.MortgageCalculator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class MortgageCalculatorImpl implements MortgageCalculator {

    public static final RoundingMode CURRENCY_ROUNDING = RoundingMode.HALF_UP;
    public static final int CURRENCY_SCALE = 2;
    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);
    private static final BigDecimal TWELVE_HUNDRED = new BigDecimal("1200");

    @Override
    public CalculationResult calculate(CalculationRequest request) {
        validateRequest(request);

        BigDecimal propertyPrice = request.propertyPrice().setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        BigDecimal downPayment = request.downPayment().setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        BigDecimal loanAmount = propertyPrice.subtract(downPayment).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        int initialTermMonths = request.termMonths();
        BigDecimal annualRate = request.annualInterestRate();
        LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now();

        BigDecimal monthlyRate = annualRate.divide(TWELVE_HUNDRED, MC);

        if (request.paymentType() == PaymentType.ANNUITY) {
            return calculateAnnuity(request, propertyPrice, downPayment, loanAmount, initialTermMonths, annualRate, monthlyRate, startDate);
        } else {
            return calculateDifferentiated(request, propertyPrice, downPayment, loanAmount, initialTermMonths, annualRate, monthlyRate, startDate);
        }
    }

    private CalculationResult calculateAnnuity(CalculationRequest request,
                                               BigDecimal propertyPrice,
                                               BigDecimal downPayment,
                                               BigDecimal loanAmount,
                                               int termMonths,
                                               BigDecimal annualRate,
                                               BigDecimal monthlyRate,
                                               LocalDate startDate) {
        List<PaymentScheduleItem> schedule = new ArrayList<>();
        BigDecimal currentBalance = loanAmount;
        BigDecimal currentAnnuityPayment = computeAnnuityPayment(currentBalance, monthlyRate, termMonths);

        BigDecimal totalInterest = BigDecimal.ZERO.setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        BigDecimal minPayment = null;
        BigDecimal maxPayment = null;
        BigDecimal firstMonthPayment = null;

        int month = 1;
        int maxLoopLimit = termMonths * 2;

        while (currentBalance.compareTo(BigDecimal.ZERO) > 0 && month <= maxLoopLimit) {
            LocalDate paymentDate = startDate.plusMonths(month);
            BigDecimal openingBalance = currentBalance;

            BigDecimal interestPayment = openingBalance.multiply(monthlyRate, MC).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);

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

            EarlyRepaymentResolution earlyResolution = resolveEarlyRepayments(request.earlyRepayments(), month, balanceAfterRegular);
            BigDecimal earlyPayment = earlyResolution.actualAmount;
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
                if (earlyResolution.strategy == RepaymentStrategy.REDUCE_PAYMENT) {
                    int remainingMonths = termMonths - month;
                    if (remainingMonths > 0) {
                        currentAnnuityPayment = computeAnnuityPayment(closingBalance, monthlyRate, remainingMonths);
                    }
                }
            }

            month++;
        }

        int actualTermMonths = schedule.size();
        BigDecimal totalPayment = loanAmount.add(totalInterest).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);

        return new CalculationResult(
                propertyPrice,
                downPayment,
                loanAmount,
                termMonths,
                actualTermMonths,
                annualRate,
                PaymentType.ANNUITY,
                firstMonthPayment != null ? firstMonthPayment : BigDecimal.ZERO,
                minPayment != null ? minPayment : BigDecimal.ZERO,
                maxPayment != null ? maxPayment : BigDecimal.ZERO,
                totalInterest,
                totalPayment,
                schedule
        );
    }

    private CalculationResult calculateDifferentiated(CalculationRequest request,
                                                      BigDecimal propertyPrice,
                                                      BigDecimal downPayment,
                                                      BigDecimal loanAmount,
                                                      int termMonths,
                                                      BigDecimal annualRate,
                                                      BigDecimal monthlyRate,
                                                      LocalDate startDate) {
        List<PaymentScheduleItem> schedule = new ArrayList<>();
        BigDecimal currentBalance = loanAmount;
        int remainingMonths = termMonths;
        BigDecimal currentPrincipalPart = currentBalance.divide(BigDecimal.valueOf(remainingMonths), CURRENCY_SCALE, CURRENCY_ROUNDING);

        BigDecimal totalInterest = BigDecimal.ZERO.setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        BigDecimal minPayment = null;
        BigDecimal maxPayment = null;
        BigDecimal firstMonthPayment = null;

        int month = 1;
        int maxLoopLimit = termMonths * 2;

        while (currentBalance.compareTo(BigDecimal.ZERO) > 0 && month <= maxLoopLimit) {
            LocalDate paymentDate = startDate.plusMonths(month);
            BigDecimal openingBalance = currentBalance;

            BigDecimal interestPayment = openingBalance.multiply(monthlyRate, MC).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);

            BigDecimal principalPayment = currentPrincipalPart;
            if (principalPayment.compareTo(currentBalance) > 0 || month == termMonths) {
                principalPayment = currentBalance;
            }

            BigDecimal balanceAfterRegular = openingBalance.subtract(principalPayment).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);

            EarlyRepaymentResolution earlyResolution = resolveEarlyRepayments(request.earlyRepayments(), month, balanceAfterRegular);
            BigDecimal earlyPayment = earlyResolution.actualAmount;
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
                if (earlyResolution.strategy == RepaymentStrategy.REDUCE_PAYMENT && remainingMonths > 0) {
                    currentPrincipalPart = closingBalance.divide(BigDecimal.valueOf(remainingMonths), CURRENCY_SCALE, CURRENCY_ROUNDING);
                }
            }

            month++;
        }

        int actualTermMonths = schedule.size();
        BigDecimal totalPayment = loanAmount.add(totalInterest).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);

        return new CalculationResult(
                propertyPrice,
                downPayment,
                loanAmount,
                termMonths,
                actualTermMonths,
                annualRate,
                PaymentType.DIFFERENTIATED,
                firstMonthPayment != null ? firstMonthPayment : BigDecimal.ZERO,
                minPayment != null ? minPayment : BigDecimal.ZERO,
                maxPayment != null ? maxPayment : BigDecimal.ZERO,
                totalInterest,
                totalPayment,
                schedule
        );
    }

    private BigDecimal computeAnnuityPayment(BigDecimal principal, BigDecimal monthlyRate, int months) {
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

    private EarlyRepaymentResolution resolveEarlyRepayments(List<EarlyRepaymentRequest> earlyRepayments, int month, BigDecimal availableBalance) {
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

    private void validateRequest(CalculationRequest request) {
        if (request.propertyPrice() == null || request.propertyPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Property price must be greater than zero");
        }
        if (request.downPayment() == null || request.downPayment().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Down payment cannot be negative");
        }
        if (request.downPayment().compareTo(request.propertyPrice()) >= 0) {
            throw new IllegalArgumentException("Down payment cannot be greater than or equal to property price");
        }
        if (request.termMonths() == null || request.termMonths() <= 0) {
            throw new IllegalArgumentException("Loan term must be at least 1 month");
        }
        if (request.annualInterestRate() == null || request.annualInterestRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Annual interest rate cannot be negative");
        }
    }

    private record EarlyRepaymentResolution(BigDecimal actualAmount, RepaymentStrategy strategy) {}
}
