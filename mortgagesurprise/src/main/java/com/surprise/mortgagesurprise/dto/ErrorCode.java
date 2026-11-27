package com.surprise.mortgagesurprise.dto;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "User already exists with this email"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    INVALID_MAGIC_LINK(HttpStatus.UNAUTHORIZED, "Invalid or expired magic link token"),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "User account is disabled"),
    PASSPORT_ALREADY_LINKED(HttpStatus.CONFLICT, "Данный паспорт уже привязан к другому профилю"),
    SCENARIO_COMPARE_COUNT(HttpStatus.CONFLICT, "You must select between 2 and 3 scenarios to compare."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please try again later."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied"),
    INVALID_OAUTH2_CODE(HttpStatus.UNAUTHORIZED, "Invalid or expired OAuth2 code"),
    AB_TEST_INVALID_DISTRIBUTION(HttpStatus.BAD_REQUEST, "A/B test variant probabilities must sum to 100"),
    AB_TEST_VARIANT_EXISTS(HttpStatus.CONFLICT, "A/B test variant already exists"),
    ADMIN_CONFIGURER_ONLY(HttpStatus.CONFLICT, "Administrator acts solely as system configurator and" +
                                                                " cannot calculate or apply for loans"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation failed");

    public final HttpStatus status;
    public final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    ErrorCode(HttpStatus status) {
        this(status, null);
    }
}
