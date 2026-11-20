package com.surprise.mortgagesurprise.service;

public interface Calculator<T, R> {
    R calculate(T request);
}
