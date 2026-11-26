package com.bank.simulator.config;

import com.surprise.common.dto.simulator.TrustEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TrustProperties Unit Tests")
class TrustPropertiesTest {

    @Test
    @DisplayName("Returns configured delta when event type is present in rules")
    void getDelta_WhenRuleExists_ReturnsConfiguredDelta() {
        TrustProperties properties = new TrustProperties(
                300,
                700,
                "secret",
                Map.of(TrustEventType.PAYMENT_ON_TIME, 15, TrustEventType.OVERDUE_90_DAYS, -200)
        );

        assertThat(properties.getDelta(TrustEventType.PAYMENT_ON_TIME)).isEqualTo(15);
        assertThat(properties.getDelta(TrustEventType.OVERDUE_90_DAYS)).isEqualTo(-200);
    }

    @Test
    @DisplayName("Returns 0 when event type is absent from rules")
    void getDelta_WhenRuleMissing_ReturnsZero() {
        TrustProperties properties = new TrustProperties(
                300,
                700,
                "secret",
                Map.of(TrustEventType.PAYMENT_ON_TIME, 15)
        );

        assertThat(properties.getDelta(TrustEventType.LOAN_CLOSED)).isZero();
    }

    @Test
    @DisplayName("Returns 0 when rules map or event type is null")
    void getDelta_WhenRulesOrTypeNull_ReturnsZero() {
        TrustProperties propertiesWithNullRules = new TrustProperties(300, 700, "secret", null);
        TrustProperties propertiesWithRules = new TrustProperties(
                300, 700, "secret", Map.of(TrustEventType.PAYMENT_ON_TIME, 15)
        );

        assertThat(propertiesWithNullRules.getDelta(TrustEventType.PAYMENT_ON_TIME)).isZero();
        assertThat(propertiesWithRules.getDelta(null)).isZero();
    }
}
