package com.surprise.mortgagesurprise.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EarlyRepaymentType {
    ONE_TIME,
    RECURRING,
    MONTHLY;

    @JsonCreator
    public static EarlyRepaymentType fromString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if ("MONTHLY".equals(normalized) || "RECURRING".equals(normalized)) {
            return RECURRING;
        }
        if ("ONE_TIME".equals(normalized)) {
            return ONE_TIME;
        }
        for (EarlyRepaymentType type : EarlyRepaymentType.values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown early repayment type: " + value);
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
