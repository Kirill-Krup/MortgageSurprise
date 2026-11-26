package com.idfinance.credit_scoring_service.exception;

public class ChainInterruptedException extends RuntimeException {
    public ChainInterruptedException(String message) {
        super(message);
    }
}