package com.surprise.mortgagesurprise.model.document;

import com.surprise.mortgagesurprise.dto.calculator.PaymentScheduleItem;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document(collection = "payment_schedules")
public class PaymentScheduleDocument {

    @Id
    private String id;

    @Indexed
    private String calculationHash;

    private BigDecimal propertyPrice;
    private BigDecimal downPayment;
    private BigDecimal loanAmount;
    private int termMonths;
    private int actualTermMonths;
    private BigDecimal annualInterestRate;
    private PaymentType paymentType;
    private BigDecimal monthlyPayment;
    private BigDecimal totalInterest;
    private BigDecimal totalPayment;
    private List<PaymentScheduleItem> items;
    private Instant calculatedAt;

    public PaymentScheduleDocument() {
    }

    public PaymentScheduleDocument(String id, String calculationHash, BigDecimal propertyPrice,
                                  BigDecimal downPayment, BigDecimal loanAmount, int termMonths,
                                  int actualTermMonths, BigDecimal annualInterestRate,
                                  PaymentType paymentType, BigDecimal monthlyPayment,
                                  BigDecimal totalInterest, BigDecimal totalPayment,
                                  List<PaymentScheduleItem> items, Instant calculatedAt) {
        this.id = id;
        this.calculationHash = calculationHash;
        this.propertyPrice = propertyPrice;
        this.downPayment = downPayment;
        this.loanAmount = loanAmount;
        this.termMonths = termMonths;
        this.actualTermMonths = actualTermMonths;
        this.annualInterestRate = annualInterestRate;
        this.paymentType = paymentType;
        this.monthlyPayment = monthlyPayment;
        this.totalInterest = totalInterest;
        this.totalPayment = totalPayment;
        this.items = items;
        this.calculatedAt = calculatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCalculationHash() {
        return calculationHash;
    }

    public void setCalculationHash(String calculationHash) {
        this.calculationHash = calculationHash;
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

    public int getActualTermMonths() {
        return actualTermMonths;
    }

    public void setActualTermMonths(int actualTermMonths) {
        this.actualTermMonths = actualTermMonths;
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

    public List<PaymentScheduleItem> getItems() {
        return items;
    }

    public void setItems(List<PaymentScheduleItem> items) {
        this.items = items;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
