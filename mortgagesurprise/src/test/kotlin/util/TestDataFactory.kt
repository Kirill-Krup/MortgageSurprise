package util

import com.surprise.common.dto.scoring.CreditDecisionResponse
import com.surprise.common.dto.scoring.LoanApplicationRequest
import com.surprise.common.dto.simulator.BlacklistCheckResponse
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationRequest
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest
import com.surprise.mortgagesurprise.model.entity.Role
import com.surprise.mortgagesurprise.model.entity.User
import com.surprise.mortgagesurprise.model.entity.UserPassportData
import com.surprise.mortgagesurprise.qa.dto.AbVariantResponse
import com.surprise.mortgagesurprise.qa.dto.ExternalRateResponse
import com.surprise.mortgagesurprise.qa.dto.MortgageRequest
import com.surprise.mortgagesurprise.qa.dto.MortgageResponse
import com.surprise.mortgagesurprise.security.CustomUserDetails
import java.util.UUID

object TestDataFactory {

    @JvmStatic
    fun createMortgageRequest(): MortgageRequest = MortgageRequest(
        propertyValue = TestConstants.PROPERTY_VALUE,
        initialPayment = TestConstants.INITIAL_PAYMENT,
        termYears = TestConstants.TERM_YEARS,
        interestRate = TestConstants.INTEREST_RATE
    )

    @JvmStatic
    fun createMortgageResponse(): MortgageResponse = MortgageResponse(
        monthlyPayment = TestConstants.EXPECTED_MONTHLY_PAYMENT,
        totalAmount = TestConstants.EXPECTED_TOTAL_AMOUNT,
        overpayment = TestConstants.EXPECTED_OVERPAYMENT
    )

    @JvmStatic
    fun createAbVariantResponse(): AbVariantResponse = AbVariantResponse(
        experimentName = TestConstants.AB_EXPERIMENT_NAME,
        color = TestConstants.AB_VARIANT_COLOR
    )

    @JvmStatic
    fun createExternalRateResponse(): ExternalRateResponse = ExternalRateResponse(
        currency = TestConstants.CURRENCY_USD,
        rate = TestConstants.USD_RATE
    )

    @JvmStatic
    @JvmOverloads
    fun createUser(
        id: UUID = TestConstants.DEFAULT_USER_ID,
        role: Role = Role.ROLE_USER
    ): User = User(
        TestConstants.USER_EMAIL,
        TestConstants.USER_PASSWORD,
        TestConstants.USER_FIRST_NAME,
        TestConstants.USER_LAST_NAME,
        role
    ).apply {
        this.id = id
    }

    @JvmStatic
    @JvmOverloads
    fun createUserDetails(
        id: UUID = TestConstants.DEFAULT_USER_ID,
        role: Role = Role.ROLE_USER
    ): CustomUserDetails = CustomUserDetails(createUser(id, role))

    @JvmStatic
    @JvmOverloads
    fun createUserPassportData(
        userId: UUID = TestConstants.DEFAULT_USER_ID
    ): UserPassportData = UserPassportData().apply {
        this.userId = userId
        this.fullName = TestConstants.APPLICANT_FULL_NAME
        this.seriesNumberEncrypted = TestConstants.PASSPORT_SERIES_ENCRYPTED
        this.passportHash = TestConstants.PASSPORT_HASH
        this.age = TestConstants.APPLICANT_AGE
        this.country = TestConstants.APPLICANT_COUNTRY
        this.city = TestConstants.APPLICANT_CITY
        this.gender = TestConstants.APPLICANT_GENDER
    }

    @JvmStatic
    fun createMicroloanCalculationRequest(): MicroloanCalculationRequest = MicroloanCalculationRequest(
        TestConstants.MICROLOAN_AMOUNT,
        TestConstants.MICROLOAN_TERM_DAYS,
        TestConstants.MICROLOAN_RATE_NAME,
        TestConstants.MICROLOAN_ENUM_TYPE,
        TestConstants.MICROLOAN_PURPOSE
    )

    @JvmStatic
    fun createMicroloanApplicationRequest(): MicroloanApplicationRequest = MicroloanApplicationRequest(
        TestConstants.MICROLOAN_AMOUNT,
        TestConstants.MICROLOAN_TERM_DAYS,
        TestConstants.MICROLOAN_PURPOSE,
        TestConstants.MICROLOAN_TYPE_BASIC,
        TestConstants.MICROLOAN_TOTAL_REPAYMENT,
        TestConstants.MICROLOAN_SCHEDULE_ID,
        TestConstants.MICROLOAN_APPLICATION_ID,
        TestConstants.APPLICANT_FULL_NAME,
        TestConstants.MICROLOAN_CURRENCY
    )

    @JvmStatic
    fun createLoanApplicationRequest(): LoanApplicationRequest = LoanApplicationRequest(
        TestConstants.APPLICANT_AGE,
        TestConstants.APPLICANT_COUNTRY,
        TestConstants.APPLICANT_CITY,
        TestConstants.APPLICANT_GENDER
    )

    @JvmStatic
    fun createApprovedScoringDecision(): CreditDecisionResponse = CreditDecisionResponse(
        true,
        null,
        TestConstants.APPROVED_SCORING_SCORE
    )

    @JvmStatic
    fun createRejectedScoringDecision(): CreditDecisionResponse = CreditDecisionResponse(
        false,
        TestConstants.SCORING_REJECTED_REASON,
        TestConstants.REJECTED_SCORING_SCORE
    )

    @JvmStatic
    fun createCleanBlacklistResponse(): BlacklistCheckResponse = BlacklistCheckResponse(
        false,
        TestConstants.BLACKLIST_CLEAN_TRUST_SCORE,
        TestConstants.BLACKLIST_CLEAN_RISK_LEVEL,
        false,
        emptyList()
    )

    @JvmStatic
    fun createBlacklistedResponse(): BlacklistCheckResponse = BlacklistCheckResponse(
        true,
        TestConstants.BLACKLIST_BLOCKED_TRUST_SCORE,
        TestConstants.BLACKLIST_BLOCKED_RISK_LEVEL,
        true,
        listOf(TestConstants.BLACKLIST_BLOCKED_REASON)
    )
}
