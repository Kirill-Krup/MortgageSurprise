package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.bank.BankOfferResponse;
import com.surprise.mortgagesurprise.service.BankOfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bank-offers")
public class BankOfferPublicController {

    private final BankOfferService bankOfferService;

    public BankOfferPublicController(BankOfferService bankOfferService) {
        this.bankOfferService = bankOfferService;
    }

    @GetMapping
    public ResponseEntity<List<BankOfferResponse>> getActiveOffers() {
        return ResponseEntity.ok(bankOfferService.getActiveOffers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankOfferResponse> getOfferById(@PathVariable UUID id) {
        return ResponseEntity.ok(bankOfferService.getOfferById(id));
    }
}
