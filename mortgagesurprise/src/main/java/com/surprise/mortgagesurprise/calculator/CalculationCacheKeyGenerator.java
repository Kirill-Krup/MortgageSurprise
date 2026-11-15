package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.util.Sha256Encoder;
import org.springframework.stereotype.Component;

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
}
