package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.MicroloanType;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.testutil.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("CalculationCacheKeyGenerator Tests")
class CalculationCacheKeyGeneratorTest {

    private CalculationCacheKeyGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        keyGenerator = new CalculationCacheKeyGenerator();
    }

    @Nested
    @DisplayName("Mortgage Key Generation")
    class MortgageKeyGeneration {

        @Test
        @DisplayName("Should generate identical 64-character SHA-256 keys for matching mortgage requests")
        void generateKey_shouldReturnDeterministicHash_whenMortgageRequestsMatch() {
            // Arrange
            CalculationRequest req1 = new CalculationRequest(
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.DOWN_PAYMENT,
                    TestConstants.TERM_MONTHS,
                    TestConstants.ANNUAL_RATE,
                    PaymentType.ANNUITY,
                    Currency.RUB,
                    LocalDate.of(2026, 1, 1),
                    List.of()
            );

            CalculationRequest req2 = new CalculationRequest(
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.DOWN_PAYMENT,
                    TestConstants.TERM_MONTHS,
                    TestConstants.ANNUAL_RATE,
                    PaymentType.ANNUITY,
                    Currency.RUB,
                    LocalDate.of(2026, 5, 1),
                    List.of()
            );

            // Act
            String key1 = keyGenerator.generateKey(req1);
            String key2 = keyGenerator.generateKey(req2);

            // Assert
            assertThat(key1)
                    .isNotNull()
                    .hasSize(64)
                    .isEqualTo(key2);
        }

        @Test
        @DisplayName("Should produce different keys when mortgage calculation parameters differ")
        void generateKey_shouldReturnDifferentHashes_whenMortgageParametersDiffer() {
            // Arrange
            CalculationRequest req1 = new CalculationRequest(
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.DOWN_PAYMENT,
                    TestConstants.TERM_MONTHS,
                    new BigDecimal("10.00"),
                    PaymentType.ANNUITY,
                    LocalDate.now(),
                    List.of()
            );

            CalculationRequest req2 = new CalculationRequest(
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.DOWN_PAYMENT,
                    TestConstants.TERM_MONTHS,
                    new BigDecimal("12.00"),
                    PaymentType.ANNUITY,
                    LocalDate.now(),
                    List.of()
            );

            // Act
            String key1 = keyGenerator.generateKey(req1);
            String key2 = keyGenerator.generateKey(req2);

            // Assert
            assertThat(key1).isNotEqualTo(key2);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when mortgage request is null")
        void generateKey_shouldThrowIllegalArgumentException_whenMortgageRequestIsNull() {
            // Arrange
            CalculationRequest nullRequest = null;

            // Act & Assert
            assertThatThrownBy(() -> keyGenerator.generateKey(nullRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Calculation request cannot be null");
        }
    }

    @Nested
    @DisplayName("Microloan Key Generation")
    class MicroloanKeyGeneration {

        @Test
        @DisplayName("Should generate deterministic keys for identical microloan requests")
        void generateKey_shouldReturnDeterministicHash_whenMicroloanRequestsMatch() {
            // Arrange
            LocalDate date = LocalDate.of(2026, 6, 1);
            MicroloanCalculationRequest req1 = new MicroloanCalculationRequest(
                    TestConstants.MICROLOAN_AMOUNT,
                    TestConstants.MICROLOAN_TERM_DAYS,
                    TestConstants.DEFAULT_RATE_NAME,
                    MicroloanType.BASIC_NEEDS,
                    "Продукты",
                    null,
                    date,
                    Currency.RUB
            );

            MicroloanCalculationRequest req2 = new MicroloanCalculationRequest(
                    TestConstants.MICROLOAN_AMOUNT,
                    TestConstants.MICROLOAN_TERM_DAYS,
                    TestConstants.DEFAULT_RATE_NAME,
                    MicroloanType.BASIC_NEEDS,
                    "Продукты",
                    null,
                    date,
                    Currency.RUB
            );

            // Act
            String key1 = keyGenerator.generateKey(req1, TestConstants.BASE_DAILY_RATE, TestConstants.BANK_INTEREST);
            String key2 = keyGenerator.generateKey(req2, TestConstants.BASE_DAILY_RATE, TestConstants.BANK_INTEREST);

            // Assert
            assertThat(key1)
                    .isNotNull()
                    .hasSize(64)
                    .isEqualTo(key2);
        }

        @Test
        @DisplayName("Should produce different keys when microloan rate parameters differ")
        void generateKey_shouldReturnDifferentHashes_whenMicroloanRatesDiffer() {
            // Arrange
            LocalDate date = LocalDate.of(2026, 6, 1);
            MicroloanCalculationRequest request = new MicroloanCalculationRequest(
                    TestConstants.MICROLOAN_AMOUNT,
                    TestConstants.MICROLOAN_TERM_DAYS,
                    TestConstants.DEFAULT_RATE_NAME,
                    MicroloanType.BASIC_NEEDS,
                    "Покупки",
                    null,
                    date,
                    Currency.RUB
            );

            // Act
            String key1 = keyGenerator.generateKey(request, new BigDecimal("0.8000"), new BigDecimal("2000.0000"));
            String key2 = keyGenerator.generateKey(request, new BigDecimal("0.9000"), new BigDecimal("2000.0000"));

            // Assert
            assertThat(key1).isNotEqualTo(key2);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when microloan request is null")
        void generateKey_shouldThrowIllegalArgumentException_whenMicroloanRequestIsNull() {
            // Arrange
            MicroloanCalculationRequest nullRequest = null;

            // Act & Assert
            assertThatThrownBy(() -> keyGenerator.generateKey(nullRequest, BigDecimal.ONE, BigDecimal.TEN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Microloan calculation request cannot be null");
        }
    }
}
