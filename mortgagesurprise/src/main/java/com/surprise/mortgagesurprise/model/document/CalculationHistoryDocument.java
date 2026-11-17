package com.surprise.mortgagesurprise.model.document;

import com.surprise.mortgagesurprise.model.enums.LoanProductType;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Document(collection = "calculation_history")
public class CalculationHistoryDocument {

    @Id
    private String id;

    @Indexed
    private UUID userId;

    private LoanProductType loanProductType = LoanProductType.MORTGAGE;
    private String scheduleDocumentId;
    private BigDecimal propertyPrice;
    private BigDecimal downPayment;
    private BigDecimal loanAmount;
    private int termMonths;
    private Integer termDays;
    private BigDecimal dailyInterestRate;
    private BigDecimal annualInterestRate;
    private PaymentType paymentType;
    private BigDecimal monthlyPayment;
    private BigDecimal totalInterest;
    private BigDecimal totalPayment;
    private com.surprise.mortgagesurprise.model.enums.Currency currency = com.surprise.mortgagesurprise.model.enums.Currency.RUB;
    private Instant createdAt;

    public CalculationHistoryDocument() {
    }

    public CalculationHistoryDocument(String id, UUID userId, String scheduleDocumentId,
                                      BigDecimal propertyPrice, BigDecimal downPayment,
                                      BigDecimal loanAmount, int termMonths,
                                      BigDecimal annualInterestRate, PaymentType paymentType,
                                      BigDecimal monthlyPayment, BigDecimal totalInterest,
                                      BigDecimal totalPayment, Instant createdAt) {
        this(id, userId, scheduleDocumentId, propertyPrice, downPayment, loanAmount, termMonths,
                annualInterestRate, paymentType, com.surprise.mortgagesurprise.model.enums.Currency.RUB,
                monthlyPayment, totalInterest, totalPayment, createdAt);
    }

    public CalculationHistoryDocument(String id, UUID userId, String scheduleDocumentId,
                                      BigDecimal propertyPrice, BigDecimal downPayment,
                                      BigDecimal loanAmount, int termMonths,
                                      BigDecimal annualInterestRate, PaymentType paymentType,
                                      com.surprise.mortgagesurprise.model.enums.Currency currency,
                                      BigDecimal monthlyPayment, BigDecimal totalInterest,
                                      BigDecimal totalPayment, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.scheduleDocumentId = scheduleDocumentId;
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
        this.createdAt = createdAt;
    }

    public CalculationHistoryDocument(String id, UUID userId, String scheduleDocumentId,
                                      BigDecimal loanAmount, int termDays, BigDecimal dailyInterestRate,
                                      BigDecimal annualInterestRate,
                                      com.surprise.mortgagesurprise.model.enums.Currency currency,
                                      BigDecimal totalInterest, BigDecimal totalPayment, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.loanProductType = LoanProductType.MICROLOAN;
        this.scheduleDocumentId = scheduleDocumentId;
        this.loanAmount = loanAmount;
        this.termDays = termDays;
        this.dailyInterestRate = dailyInterestRate;
        this.annualInterestRate = annualInterestRate;
        this.currency = currency != null ? currency : com.surprise.mortgagesurprise.model.enums.Currency.RUB;
        this.totalInterest = totalInterest;
        this.totalPayment = totalPayment;
        this.createdAt = createdAt;
    }

    public LoanProductType getLoanProductType() {
        return loanProductType;
    }

    public void setLoanProductType(LoanProductType loanProductType) {
        this.loanProductType = loanProductType;
    }

    public Integer getTermDays() {
        return termDays;
    }

    public void setTermDays(Integer termDays) {
        this.termDays = termDays;
    }

    public BigDecimal getDailyInterestRate() {
        return dailyInterestRate;
    }

    public void setDailyInterestRate(BigDecimal dailyInterestRate) {
        this.dailyInterestRate = dailyInterestRate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getScheduleDocumentId() {
        return scheduleDocumentId;
    }

    public void setScheduleDocumentId(String scheduleDocumentId) {
        this.scheduleDocumentId = scheduleDocumentId;
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
}
