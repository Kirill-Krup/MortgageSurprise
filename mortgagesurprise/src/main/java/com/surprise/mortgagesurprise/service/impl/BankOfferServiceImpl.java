package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.bank.BankOfferRequest;
import com.surprise.mortgagesurprise.dto.bank.BankOfferResponse;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.messaging.event.RateChangedEvent;
import com.surprise.mortgagesurprise.messaging.sender.RabbitMqEventSender;
import com.surprise.mortgagesurprise.model.entity.BankOffer;
import com.surprise.mortgagesurprise.repository.jpa.BankOfferRepository;
import com.surprise.mortgagesurprise.service.BankOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class BankOfferServiceImpl implements BankOfferService {

    private final BankOfferRepository bankOfferRepository;
    private final RabbitMqEventSender<RateChangedEvent> messageSender;

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

        if (hasRateChanged(oldRate, newRate)) {
            RateChangedEvent event = new RateChangedEvent(
                    updated.getId(),
                    updated.getBankName(),
                    oldRate,
                    newRate,
                    Instant.now()
            );
            sendAfterCommit(event);
        }

        return BankOfferResponse.from(updated);
    }

    private boolean hasRateChanged(BigDecimal oldRate, BigDecimal newRate) {
        if (oldRate == null && newRate == null) {
            return false;
        }
        if (oldRate == null || newRate == null) {
            return true;
        }
        return oldRate.compareTo(newRate) != 0;
    }

    private void sendAfterCommit(RateChangedEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messageSender.send(event);
                }
            });
        } else {
            messageSender.send(event);
        }
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
