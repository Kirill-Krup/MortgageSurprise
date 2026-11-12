package com.surprise.mortgagesurprise.dto.bank;

import com.surprise.mortgagesurprise.model.entity.BankOffer;
import com.surprise.mortgagesurprise.model.enums.Currency;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BankOfferResponse(
        UUID id,
        String bankName,
        String offerName,
        BigDecimal annualInterestRate,
        BigDecimal minDownPaymentPercent,
        int maxTermMonths,
        BigDecimal minLoanAmount,
        BigDecimal maxLoanAmount,
        Currency currency,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public static BankOfferResponse from(BankOffer offer) {
        return new BankOfferResponse(
                offer.getId(),
                offer.getBankName(),
                offer.getOfferName(),
                offer.getAnnualInterestRate(),
                offer.getMinDownPaymentPercent(),
                offer.getMaxTermMonths(),
                offer.getMinLoanAmount(),
                offer.getMaxLoanAmount(),
                offer.getCurrency() != null ? offer.getCurrency() : Currency.RUB,
                offer.isActive(),
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }
}
