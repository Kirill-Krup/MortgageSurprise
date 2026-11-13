package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.bank.BankOfferRequest;
import com.surprise.mortgagesurprise.dto.bank.BankOfferResponse;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.messaging.event.RateChangedEvent;
import com.surprise.mortgagesurprise.messaging.producer.MessageProducer;
import com.surprise.mortgagesurprise.model.entity.BankOffer;
import com.surprise.mortgagesurprise.repository.jpa.BankOfferRepository;
import com.surprise.mortgagesurprise.service.BankOfferService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BankOfferServiceImpl implements BankOfferService {

    private final BankOfferRepository bankOfferRepository;
    private final MessageProducer messageProducer;

    public BankOfferServiceImpl(BankOfferRepository bankOfferRepository, MessageProducer messageProducer) {
        this.bankOfferRepository = bankOfferRepository;
        this.messageProducer = messageProducer;
    }

    @Override
    public BankOfferResponse createOffer(BankOfferRequest req) {
        BankOffer offer = new BankOffer(
                req.bankName(),
                req.offerName(),
                req.annualInterestRate(),
                req.minDownPaymentPercent(),
                req.maxTermMonths(),
                req.minLoanAmount(),
                req.maxLoanAmount(),
                req.currency()
        );
        BankOffer saved = bankOfferRepository.save(offer);
        return BankOfferResponse.from(saved);
    }

    @Override
    public BankOfferResponse updateOffer(UUID id, BankOfferRequest req) {
        BankOffer offer = bankOfferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank offer not found: " + id));

        BigDecimal oldRate = offer.getAnnualInterestRate();
        BigDecimal newRate = req.annualInterestRate();

        offer.setBankName(req.bankName());
        offer.setOfferName(req.offerName());
        offer.setAnnualInterestRate(newRate);
        offer.setMinDownPaymentPercent(req.minDownPaymentPercent());
        offer.setMaxTermMonths(req.maxTermMonths());
        offer.setMinLoanAmount(req.minLoanAmount());
        offer.setMaxLoanAmount(req.maxLoanAmount());
        if (req.currency() != null) {
            offer.setCurrency(req.currency());
        }

        BankOffer updated = bankOfferRepository.save(offer);

        if (oldRate.compareTo(newRate) != 0) {
            RateChangedEvent event = new RateChangedEvent(
                    updated.getId(),
                    updated.getBankName(),
                    oldRate,
                    newRate,
                    Instant.now()
            );
            messageProducer.sendRateChangedEvent(event);
        }

        return BankOfferResponse.from(updated);
    }

    @Override
    public void deleteOffer(UUID id) {
        BankOffer offer = bankOfferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank offer not found: " + id));
        bankOfferRepository.delete(offer);
    }

    @Override
    @Transactional(readOnly = true)
    public BankOfferResponse getOfferById(UUID id) {
        return bankOfferRepository.findById(id)
                .map(BankOfferResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Bank offer not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankOfferResponse> getAllOffers() {
        return bankOfferRepository.findAll().stream()
                .map(BankOfferResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankOfferResponse> getActiveOffers() {
        return bankOfferRepository.findByActiveTrue().stream()
                .map(BankOfferResponse::from)
                .toList();
    }
}
