package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.bank.BankOfferRequest;
import com.surprise.mortgagesurprise.dto.bank.BankOfferResponse;
import com.surprise.mortgagesurprise.service.BankOfferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<BankOfferResponse>> getOffers(@RequestParam(value = "all", defaultValue = "false") boolean all) {
        if (all) {
            return ResponseEntity.ok(bankOfferService.getAllOffers());
        }
        return ResponseEntity.ok(bankOfferService.getActiveOffers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankOfferResponse> getOfferById(@PathVariable UUID id) {
        return ResponseEntity.ok(bankOfferService.getOfferById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BankOfferResponse> createOffer(@Valid @RequestBody BankOfferRequest request) {
        BankOfferResponse response = bankOfferService.createOffer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BankOfferResponse> updateOffer(
            @PathVariable UUID id,
            @Valid @RequestBody BankOfferRequest request
    ) {
        BankOfferResponse response = bankOfferService.updateOffer(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOffer(@PathVariable UUID id) {
        bankOfferService.deleteOffer(id);
        return ResponseEntity.noContent().build();
    }
}
