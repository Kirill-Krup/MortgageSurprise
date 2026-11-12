package com.surprise.mortgagesurprise.model.entity;

import com.surprise.mortgagesurprise.model.enums.PaymentType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calculation_scenarios")
public class CalculationScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String title;

    @Column(name = "property_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal propertyPrice;

    @Column(name = "down_payment", nullable = false, precision = 15, scale = 2)
    private BigDecimal downPayment;

    @Column(name = "loan_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal loanAmount;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Column(name = "annual_interest_rate", nullable = false, precision = 6, scale = 2)
    private BigDecimal annualInterestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private com.surprise.mortgagesurprise.model.enums.Currency currency = com.surprise.mortgagesurprise.model.enums.Currency.RUB;

    @Column(name = "monthly_payment", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "total_interest", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "total_payment", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPayment;

    @Column(name = "mongo_schedule_id")
    private String mongoScheduleId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public CalculationScenario() {
    }

    public CalculationScenario(UUID userId, String title, BigDecimal propertyPrice,
                               BigDecimal downPayment, BigDecimal loanAmount, int termMonths,
                               BigDecimal annualInterestRate, PaymentType paymentType,
                               BigDecimal monthlyPayment, BigDecimal totalInterest,
                               BigDecimal totalPayment, String mongoScheduleId) {
        this(userId, title, propertyPrice, downPayment, loanAmount, termMonths, annualInterestRate,
                paymentType, com.surprise.mortgagesurprise.model.enums.Currency.RUB,
                monthlyPayment, totalInterest, totalPayment, mongoScheduleId);
    }

    public CalculationScenario(UUID userId, String title, BigDecimal propertyPrice,
                               BigDecimal downPayment, BigDecimal loanAmount, int termMonths,
                               BigDecimal annualInterestRate, PaymentType paymentType,
                               com.surprise.mortgagesurprise.model.enums.Currency currency,
                               BigDecimal monthlyPayment, BigDecimal totalInterest,
                               BigDecimal totalPayment, String mongoScheduleId) {
        this.userId = userId;
        this.title = title;
        this.propertyPrice = propertyPrice;
        this.downPayment = downPayment;
        this.loanAmount = loanAmount;
        this.termMonths = termMonths;
        this.annualInterestRate = annualInterestRate;
        this.paymentType = paymentType;
        this.currency = currency != null ? currency : com.surprise.mortgagesurprise.model.enums.Currency.RUB;
        this.monthlyPayment = monthlyPayment;
        this.totalInterest = totalInterest;
        this.totalPayment = totalPayment;
        this.mongoScheduleId = mongoScheduleId;
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getPropertyPrice() {
        return propertyPrice;
    }

    public void setPropertyPrice(BigDecimal propertyPrice) {
        this.propertyPrice = propertyPrice;
    }

    public BigDecimal getDownPayment() {
        return downPayment;
    }

    public void setDownPayment(BigDecimal downPayment) {
        this.downPayment = downPayment;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(int termMonths) {
        this.termMonths = termMonths;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public void setAnnualInterestRate(BigDecimal annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(BigDecimal monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public BigDecimal getTotalInterest() {
        return totalInterest;
    }

    public void setTotalInterest(BigDecimal totalInterest) {
        this.totalInterest = totalInterest;
    }

    public BigDecimal getTotalPayment() {
        return totalPayment;
    }

    public void setTotalPayment(BigDecimal totalPayment) {
        this.totalPayment = totalPayment;
    }

    public String getMongoScheduleId() {
        return mongoScheduleId;
    }

    public void setMongoScheduleId(String mongoScheduleId) {
        this.mongoScheduleId = mongoScheduleId;
    }

    public com.surprise.mortgagesurprise.model.enums.Currency getCurrency() {
        return currency;
    }

    public void setCurrency(com.surprise.mortgagesurprise.model.enums.Currency currency) {
        this.currency = currency;
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
