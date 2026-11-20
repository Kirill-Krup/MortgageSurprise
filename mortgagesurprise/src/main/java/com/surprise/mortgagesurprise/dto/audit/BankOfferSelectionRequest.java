package com.surprise.mortgagesurprise.dto.audit;

import com.surprise.mortgagesurprise.model.enums.Currency;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BankOfferSelectionRequest(
        @NotNull(message = "Bank offer ID is required")
        Long bankOfferId,
        String bankName,
        String offerName,
        BigDecimal annualInterestRate,
        BigDecimal minDownPaymentPercent,
        Integer maxTermMonths,
        Currency currency
) {}
