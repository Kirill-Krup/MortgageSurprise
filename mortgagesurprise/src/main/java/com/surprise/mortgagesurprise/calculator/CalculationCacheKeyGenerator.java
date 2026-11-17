package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.util.Sha256Encoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Single responsibility: Generate deterministic cache keys for calculation requests.
 */
@Component
public class CalculationCacheKeyGenerator {

    public String generateKey(CalculationRequest r) {
        if (r == null) {
            throw new IllegalArgumentException("Calculation request cannot be null");
        }
        String raw = r.propertyPrice() + ":" + r.downPayment() + ":" + r.termMonths() + ":"
                + r.annualInterestRate() + ":" + r.paymentType() + ":" + r.currency() + ":"
                + (r.earlyRepayments() != null ? r.earlyRepayments().hashCode() : 0);
        return Sha256Encoder.toSha256(raw);
    }

    public String generateKey(MicroloanCalculationRequest r, BigDecimal baseRate, BigDecimal surchargeRate) {
        if (r == null) {
            throw new IllegalArgumentException("Microloan calculation request cannot be null");
        }
        String purpose = r.purpose() != null ? r.purpose().trim().toLowerCase() : "";
        String raw = "MICROLOAN:" + r.amount() + ":" + r.termDays() + ":"
                + purpose + ":" + baseRate + ":" + surchargeRate + ":"
                + r.currency() + ":" + r.startDate();
        return Sha256Encoder.toSha256(raw);
    }

    public String generateKey(MicroloanCalculationRequest r) {
        return generateKey(r, new BigDecimal("0.8000"), new BigDecimal("5.0000"));
    }
}
