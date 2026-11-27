package com.bank.simulator.service;

public interface DocumentHashService {
    String hash(String documentNumber);
    String normalize(String raw);
}
