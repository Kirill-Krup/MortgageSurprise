package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.bank.BankOfferRequest;
import com.surprise.mortgagesurprise.dto.bank.BankOfferResponse;

import java.util.List;
import java.util.UUID;

public interface BankOfferService {
    BankOfferResponse createOffer(BankOfferRequest req);
    BankOfferResponse updateOffer(UUID id, BankOfferRequest req);
    void deleteOffer(UUID id);
    BankOfferResponse getOfferById(UUID id);
    List<BankOfferResponse> getAllOffers();
    List<BankOfferResponse> getActiveOffers();
}
