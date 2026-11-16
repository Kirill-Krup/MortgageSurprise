package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculationCacheKeyGeneratorTest {

    private CalculationCacheKeyGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        keyGenerator = new CalculationCacheKeyGenerator();
    }

    @Test
    @DisplayName("Identical calculation requests generate identical cache keys")
    void testDeterministicKey() {
        CalculationRequest req1 = new CalculationRequest(
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                240,
                new BigDecimal("10.00"),
                PaymentType.ANNUITY,
                Currency.RUB,
                LocalDate.of(2026, 1, 1),
                List.of()
        );

        CalculationRequest req2 = new CalculationRequest(
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                240,
                new BigDecimal("10.00"),
                PaymentType.ANNUITY,
                Currency.RUB,
                LocalDate.of(2026, 5, 1),
                List.of()
        );

        String key1 = keyGenerator.generateKey(req1);
        String key2 = keyGenerator.generateKey(req2);

        assertNotNull(key1);
        assertEquals(64, key1.length()); // SHA-256 hex string length
        assertEquals(key1, key2);
    }

    @Test
    @DisplayName("Different calculation parameters produce different keys")
    void testDifferentParametersDifferentKeys() {
        CalculationRequest req1 = new CalculationRequest(
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                240,
                new BigDecimal("10.00"),
                PaymentType.ANNUITY,
                LocalDate.now(),
                List.of()
        );

        CalculationRequest req2 = new CalculationRequest(
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                240,
                new BigDecimal("12.00"),
                PaymentType.ANNUITY,
                LocalDate.now(),
                List.of()
        );

        assertNotEquals(keyGenerator.generateKey(req1), keyGenerator.generateKey(req2));
    }
}
