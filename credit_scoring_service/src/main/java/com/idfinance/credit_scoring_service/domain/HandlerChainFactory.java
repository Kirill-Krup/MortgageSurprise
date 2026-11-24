package com.idfinance.credit_scoring_service.domain;

public interface HandlerChainFactory<T> {
    T create();
}
