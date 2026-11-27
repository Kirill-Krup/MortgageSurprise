package com.surprise.mortgagesurprise.model.entity;

import com.surprise.mortgagesurprise.model.enums.PaymentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calculation_scenarios")
@Getter
@Setter
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
}
