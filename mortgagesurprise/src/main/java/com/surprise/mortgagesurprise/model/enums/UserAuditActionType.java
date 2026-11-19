package com.surprise.mortgagesurprise.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum UserAuditActionType {
    MORTGAGE_CALCULATION,
    MICROLOAN_CALCULATION,
    MICROLOAN_APPLICATION,
    BANK_OFFER_SELECTED,
    MICROLOAN_APPLIED,
    SCENARIO_CALCULATED,
    SCENARIO_SAVED;

    public UserAuditActionType canonical() {
        return switch (this) {
            case MICROLOAN_APPLIED -> MICROLOAN_APPLICATION;
            case SCENARIO_CALCULATED -> MORTGAGE_CALCULATION;
            default -> this;
        };
    }

    @JsonCreator
    public static UserAuditActionType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "MORTGAGE_CALCULATION", "SCENARIO_CALCULATED", "CALCULATE", "MORTGAGE" -> MORTGAGE_CALCULATION;
            case "MICROLOAN_CALCULATION", "MICROLOAN_CALCULATED" -> MICROLOAN_CALCULATION;
            case "MICROLOAN_APPLICATION", "MICROLOAN_APPLIED", "APPLY_MICROLOAN" -> MICROLOAN_APPLICATION;
            case "BANK_OFFER_SELECTED", "OFFER_SELECTED" -> BANK_OFFER_SELECTED;
            case "SCENARIO_SAVED", "SAVE_SCENARIO" -> SCENARIO_SAVED;
            default -> {
                for (UserAuditActionType type : values()) {
                    if (type.name().equalsIgnoreCase(normalized)) {
                        yield type;
                    }
                }
                yield null;
            }
        };
    }
}
