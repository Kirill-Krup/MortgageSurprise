package com.surprise.mortgagesurprise.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_offers")
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
                     BigDecimal minLoanAmount, BigDecimal maxLoanAmount) {
        this(bankName, offerName, annualInterestRate, minDownPaymentPercent, maxTermMonths,
                minLoanAmount, maxLoanAmount, com.surprise.mortgagesurprise.model.enums.Currency.RUB);
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getOfferName() {
        return offerName;
    }

    public void setOfferName(String offerName) {
        this.offerName = offerName;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public void setAnnualInterestRate(BigDecimal annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }

    public BigDecimal getMinDownPaymentPercent() {
        return minDownPaymentPercent;
    }

    public void setMinDownPaymentPercent(BigDecimal minDownPaymentPercent) {
        this.minDownPaymentPercent = minDownPaymentPercent;
    }

    public int getMaxTermMonths() {
        return maxTermMonths;
    }

    public void setMaxTermMonths(int maxTermMonths) {
        this.maxTermMonths = maxTermMonths;
    }

    public BigDecimal getMinLoanAmount() {
        return minLoanAmount;
    }

    public void setMinLoanAmount(BigDecimal minLoanAmount) {
        this.minLoanAmount = minLoanAmount;
    }

    public BigDecimal getMaxLoanAmount() {
        return maxLoanAmount;
    }

    public void setMaxLoanAmount(BigDecimal maxLoanAmount) {
        this.maxLoanAmount = maxLoanAmount;
    }

    public com.surprise.mortgagesurprise.model.enums.Currency getCurrency() {
        return currency;
    }

    public void setCurrency(com.surprise.mortgagesurprise.model.enums.Currency currency) {
        this.currency = currency;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
