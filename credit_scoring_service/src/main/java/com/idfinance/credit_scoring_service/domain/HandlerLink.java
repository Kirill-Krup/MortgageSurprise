package com.idfinance.credit_scoring_service.domain;

public interface HandlerLink<T, E, R extends HandlerLink<T, E, R>> {
    R link(R first, R... chain);

    T handle(E loanApplication);

    T handleNext(E loanApplication);
}