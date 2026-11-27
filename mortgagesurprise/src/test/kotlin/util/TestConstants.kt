package util

import com.surprise.common.dto.simulator.RiskLevel
import com.surprise.mortgagesurprise.model.enums.Currency
import com.surprise.mortgagesurprise.model.enums.MicroloanType
import java.math.BigDecimal
import java.util.UUID

object TestConstants {

    // Mortgage calculation
    const val PROPERTY_VALUE: Double = 5_000_000.0
    const val INITIAL_PAYMENT: Double = 1_000_000.0
    const val TERM_YEARS: Int = 20
    const val INTEREST_RATE: Double = 12.0
    const val EXPECTED_MONTHLY_PAYMENT: Double = 55054.31
    const val EXPECTED_TOTAL_AMOUNT: Double = 13213034.4
    const val EXPECTED_OVERPAYMENT: Double = 8213034.4
    const val CALCULATE_MORTGAGE_ENDPOINT: String = "/api/v1/calculator/calculate"

    // A/B testing
    const val AB_EXPERIMENT_NAME: String = "mortgage_calculate_button"
    const val AB_VARIANT_COLOR: String = "blue"
    const val AB_VARIANT_ENDPOINT: String = "/api/v1/ab-test/variant"

    // External rates
    const val CURRENCY_USD: String = "USD"
    const val CURRENCY_USD_LOWER: String = "usd"
    const val USD_RATE: Double = 93.45
    const val EXTERNAL_RATE_USD_ENDPOINT: String = "/external/rates/usd"

    // User & Passport
    @JvmField
    val DEFAULT_USER_ID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
    const val USER_EMAIL: String = "user@example.com"
    const val USER_PASSWORD: String = "pass"
    const val USER_FIRST_NAME: String = "Ivan"
    const val USER_LAST_NAME: String = "Ivanov"
    const val APPLICANT_FULL_NAME: String = "Ivan Ivanov"
    const val APPLICANT_AGE: Int = 29
    const val APPLICANT_COUNTRY: String = "RU"
    const val APPLICANT_CITY: String = "Moscow"
    const val APPLICANT_GENDER: String = "MALE"
    const val PASSPORT_HASH: String = "test-passport-hash-0001"
    const val PASSPORT_SERIES_ENCRYPTED: String = "encrypted-series-number"

    // Microloan
    @JvmField
    val MICROLOAN_AMOUNT: BigDecimal = BigDecimal.valueOf(10_000)
    const val MICROLOAN_TERM_DAYS: Int = 14
    const val MICROLOAN_PURPOSE: String = "Groceries"
    const val MICROLOAN_RATE_NAME: String = "STANDARD"
    @JvmField
    val MICROLOAN_ENUM_TYPE: MicroloanType = MicroloanType.BASIC_NEEDS
    const val MICROLOAN_TYPE_BASIC: String = "BASIC"
    @JvmField
    val MICROLOAN_TOTAL_REPAYMENT: BigDecimal = BigDecimal.valueOf(11_000)
    const val MICROLOAN_SCHEDULE_ID: String = "sched-1"
    const val MICROLOAN_APPLICATION_ID: String = "MS-11111"
    @JvmField
    val MICROLOAN_CURRENCY: Currency = Currency.RUB

    // Credit Scoring & Blacklist
    const val APPROVED_SCORING_SCORE: Float = 0.85f
    const val REJECTED_SCORING_SCORE: Float = 0f
    const val SCORING_REJECTED_REASON: String = "Your credit score is too low"
    const val BLACKLIST_CLEAN_TRUST_SCORE: Int = 80
    const val BLACKLIST_BLOCKED_TRUST_SCORE: Int = 10
    @JvmField
    val BLACKLIST_CLEAN_RISK_LEVEL: RiskLevel = RiskLevel.LOW
    @JvmField
    val BLACKLIST_BLOCKED_RISK_LEVEL: RiskLevel = RiskLevel.HIGH
    const val BLACKLIST_BLOCKED_REASON: String = "Fraud history"
}
