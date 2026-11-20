package com.surprise.mortgagesurprise.testutil;

import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Centralized constants for test suites.
 */
public final class TestConstants {

    private TestConstants() {
    }

    // Common UUIDs and Strings
    public static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final String USER_EMAIL = "testuser@example.com";
    public static final String SCHEDULE_ID = "sched-test-123";
    public static final String CALCULATION_HASH = "mock-sha256-hash-0123456789abcdef";

    // Mortgage Constants
    public static final BigDecimal PROPERTY_PRICE = new BigDecimal("5000000.00");
    public static final BigDecimal DOWN_PAYMENT = new BigDecimal("1000000.00");
    public static final BigDecimal LOAN_AMOUNT = new BigDecimal("4000000.00");
    public static final int TERM_MONTHS = 240;
    public static final BigDecimal ANNUAL_RATE = new BigDecimal("10.00");
    public static final PaymentType DEFAULT_PAYMENT_TYPE = PaymentType.ANNUITY;
    public static final Currency DEFAULT_CURRENCY = Currency.RUB;
    public static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);

    // Microloan Constants
    public static final BigDecimal MICROLOAN_AMOUNT = new BigDecimal("10000.00");
    public static final int MICROLOAN_TERM_DAYS = 10;
    public static final String DEFAULT_RATE_NAME = "Standard Rate";
    public static final BigDecimal BASE_DAILY_RATE = new BigDecimal("0.8000");
    public static final BigDecimal BANK_INTEREST = new BigDecimal("2000.0000");
}
