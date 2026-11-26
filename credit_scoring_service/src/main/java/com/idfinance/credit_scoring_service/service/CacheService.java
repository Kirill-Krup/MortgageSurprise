package com.idfinance.credit_scoring_service.service;

import com.idfinance.credit_scoring_service.domain.rule_set.Identifiable;

import java.util.Set;

public interface CacheService {
    <T extends Identifiable<?>> void put(String key, T value);

    <T extends Identifiable<?>> void putAll(String key, Set<T> value);

    <T> T get(String key);

    void remove(String key);
}