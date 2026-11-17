package com.surprise.mortgagesurprise.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_offers")
@Getter
@Setter
public class BankOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "offer_name", nullable = false)
    private String offerName;

    @Column(name = "annual_interest_rate", nullable = false, precision = 6, scale = 2)
    private BigDecimal annualInterestRate;

    @Column(name = "min_down_payment_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal minDownPaymentPercent;

    @Column(name = "max_term_months", nullable = false)
    private int maxTermMonths;

    @Column(name = "min_loan_amount", precision = 15, scale = 2)
    private BigDecimal minLoanAmount;

    @Column(name = "max_loan_amount", precision = 15, scale = 2)
    private BigDecimal maxLoanAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private com.surprise.mortgagesurprise.model.enums.Currency currency = com.surprise.mortgagesurprise.model.enums.Currency.RUB;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public BankOffer() {
    }

    public BankOffer(String bankName, String offerName, BigDecimal annualInterestRate,
                     BigDecimal minDownPaymentPercent, int maxTermMonths,
                     BigDecimal minLoanAmount, BigDecimal maxLoanAmount,
                     com.surprise.mortgagesurprise.model.enums.Currency currency) {
        this.bankName = bankName;
        this.offerName = offerName;
        this.annualInterestRate = annualInterestRate;
        this.minDownPaymentPercent = minDownPaymentPercent;
        this.maxTermMonths = maxTermMonths;
        this.minLoanAmount = minLoanAmount;
        this.maxLoanAmount = maxLoanAmount;
        this.currency = currency != null ? currency : com.surprise.mortgagesurprise.model.enums.Currency.RUB;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
