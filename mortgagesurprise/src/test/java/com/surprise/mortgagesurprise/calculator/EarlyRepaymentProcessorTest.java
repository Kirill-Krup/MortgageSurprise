package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.dto.calculator.EarlyRepaymentRequest;
import com.surprise.mortgagesurprise.model.enums.EarlyRepaymentType;
import com.surprise.mortgagesurprise.model.enums.RepaymentStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EarlyRepaymentProcessorTest {

    private EarlyRepaymentProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new EarlyRepaymentProcessor();
    }

    @Test
    @DisplayName("Empty or null early repayments return zero")
    void testEmptyRepayments() {
        EarlyRepaymentResolution res1 = processor.resolve(null, 1, new BigDecimal("100000.00"));
        assertEquals(new BigDecimal("0.00"), res1.actualAmount());

        EarlyRepaymentResolution res2 = processor.resolve(List.of(), 1, new BigDecimal("100000.00"));
        assertEquals(new BigDecimal("0.00"), res2.actualAmount());

        EarlyRepaymentResolution res3 = processor.resolve(List.of(
                new EarlyRepaymentRequest(1, null, new BigDecimal("10000.00"), RepaymentStrategy.REDUCE_TERM, EarlyRepaymentType.ONE_TIME)
        ), 1, BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), res3.actualAmount());
    }

    @Test
    @DisplayName("One-time early repayment matches exact start month")
    void testOneTimeRepayment() {
        List<EarlyRepaymentRequest> requests = List.of(
                new EarlyRepaymentRequest(3, null, new BigDecimal("50000.00"), RepaymentStrategy.REDUCE_TERM, EarlyRepaymentType.ONE_TIME)
        );

        EarlyRepaymentResolution month2 = processor.resolve(requests, 2, new BigDecimal("200000.00"));
        assertEquals(new BigDecimal("0.00"), month2.actualAmount());

        EarlyRepaymentResolution month3 = processor.resolve(requests, 3, new BigDecimal("200000.00"));
        assertEquals(new BigDecimal("50000.00"), month3.actualAmount());
        assertEquals(RepaymentStrategy.REDUCE_TERM, month3.strategy());

        EarlyRepaymentResolution month4 = processor.resolve(requests, 4, new BigDecimal("200000.00"));
        assertEquals(new BigDecimal("0.00"), month4.actualAmount());
    }

    @Test
    @DisplayName("Recurring early repayment applies within start and end months")
    void testRecurringRepayment() {
        List<EarlyRepaymentRequest> requests = List.of(
                new EarlyRepaymentRequest(2, 4, new BigDecimal("25000.00"), RepaymentStrategy.REDUCE_PAYMENT, EarlyRepaymentType.RECURRING)
        );

        assertEquals(new BigDecimal("0.00"), processor.resolve(requests, 1, new BigDecimal("200000.00")).actualAmount());
        assertEquals(new BigDecimal("25000.00"), processor.resolve(requests, 2, new BigDecimal("200000.00")).actualAmount());
        assertEquals(new BigDecimal("25000.00"), processor.resolve(requests, 3, new BigDecimal("200000.00")).actualAmount());
        assertEquals(new BigDecimal("25000.00"), processor.resolve(requests, 4, new BigDecimal("200000.00")).actualAmount());
        assertEquals(new BigDecimal("0.00"), processor.resolve(requests, 5, new BigDecimal("200000.00")).actualAmount());
    }

    @Test
    @DisplayName("Early repayment is capped by available balance")
    void testCappedByAvailableBalance() {
        List<EarlyRepaymentRequest> requests = List.of(
                new EarlyRepaymentRequest(1, null, new BigDecimal("100000.00"), RepaymentStrategy.REDUCE_TERM, EarlyRepaymentType.ONE_TIME)
        );

        EarlyRepaymentResolution result = processor.resolve(requests, 1, new BigDecimal("35000.00"));
        assertEquals(new BigDecimal("35000.00"), result.actualAmount());
    }
}
