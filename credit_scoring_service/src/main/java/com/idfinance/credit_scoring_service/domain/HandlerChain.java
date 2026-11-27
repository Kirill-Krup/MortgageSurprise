package com.idfinance.credit_scoring_service.domain;

public interface HandlerChain<T, E> {
    T executeChain(E e);
}