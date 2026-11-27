package com.surprise.common.dto.simulator;

public enum TrustEventType {
    PAYMENT_ON_TIME,
    PAYMENT_DELAY,
    OVERDUE_30_DAYS,
    OVERDUE_60_DAYS,
    OVERDUE_90_DAYS,
    LOAN_CLOSED,
    HARD_BLOCK_MANUAL,
    HARD_UNBLOCK_MANUAL,
    FRAUD_SUSPECTED,
    BANKRUPTCY
}
