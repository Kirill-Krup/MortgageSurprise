package com.idfinance.credit_scoring_service.exception;

public class NonUniqueOrderException extends RuntimeException {
    public NonUniqueOrderException(String message) {
        super(message);
    }
}