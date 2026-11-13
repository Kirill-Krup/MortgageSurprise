package com.surprise.mortgagesurprise.dto.bank;

import com.surprise.mortgagesurprise.model.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BankOfferRequest(
        @NotBlank(message = "Bank name is required")
        String bankName,

        @NotBlank(message = "Offer name is required")
        String offerName,

        @NotNull(message = "Annual interest rate is required")
        @DecimalMin(value = "0.01", message = "Interest rate must be greater than 0")
        BigDecimal annualInterestRate,

        @NotNull(message = "Minimum down payment percent is required")
        @DecimalMin(value = "0.00", message = "Down payment percent cannot be negative")
        BigDecimal minDownPaymentPercent,

        @NotNull(message = "Maximum term in months is required")
        @Min(value = 1, message = "Maximum term must be at least 1 month")
        Integer maxTermMonths,

        BigDecimal minLoanAmount,
        BigDecimal maxLoanAmount,
        Currency currency
) {
    public BankOfferRequest {
        if (currency == null) {
            currency = Currency.RUB;
        }
    }

    public BankOfferRequest(String bankName, String offerName, BigDecimal annualInterestRate,
                            BigDecimal minDownPaymentPercent, Integer maxTermMonths,
                            BigDecimal minLoanAmount, BigDecimal maxLoanAmount) {
        this(bankName, offerName, annualInterestRate, minDownPaymentPercent, maxTermMonths, minLoanAmount, maxLoanAmount, Currency.RUB);
    }
}
