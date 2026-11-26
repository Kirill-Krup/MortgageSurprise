package com.bank.simulator.config;

import com.surprise.common.dto.simulator.TrustEventType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;

@ConfigurationProperties(prefix = "trust")
public record TrustProperties(
        @DefaultValue("300") int blacklistThreshold,
        @DefaultValue("700") int defaultInitialScore,
        @DefaultValue("HeHeHeNoKeyHere=)") String hmacSecret,
        Map<TrustEventType, Integer> rules
) {

    public int getDelta(TrustEventType type) {
        if (rules == null || type == null) {
            return 0;
        }
        return rules.getOrDefault(type, 0);
    }
}
