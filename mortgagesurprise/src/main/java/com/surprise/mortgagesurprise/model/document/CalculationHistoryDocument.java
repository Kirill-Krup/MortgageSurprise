package com.surprise.mortgagesurprise.model.document;

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

    private String scheduleDocumentId;
    private BigDecimal propertyPrice;
    private BigDecimal downPayment;
    private BigDecimal loanAmount;
    private int termMonths;
    private BigDecimal annualInterestRate;
    private PaymentType paymentType;
    private BigDecimal monthlyPayment;
    private BigDecimal totalInterest;
    private BigDecimal totalPayment;
    private Instant createdAt;

    public CalculationHistoryDocument() {
    }

    public CalculationHistoryDocument(String id, UUID userId, String scheduleDocumentId,
                                      BigDecimal propertyPrice, BigDecimal downPayment,
                                      BigDecimal loanAmount, int termMonths,
                                      BigDecimal annualInterestRate, PaymentType paymentType,
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
        this.monthlyPayment = monthlyPayment;
        this.totalInterest = totalInterest;
        this.totalPayment = totalPayment;
        this.createdAt = createdAt;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
