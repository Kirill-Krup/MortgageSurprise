package com.surprise.mortgagesurprise.model.document;

import com.surprise.mortgagesurprise.dto.calculator.PaymentScheduleItem;
import com.surprise.mortgagesurprise.dto.calculator.microloan.DailyScheduleItem;
import com.surprise.mortgagesurprise.model.enums.LoanProductType;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Document(collection = "payment_schedules")
public class PaymentScheduleDocument {

    @Id
    private String id;

    @Indexed
    private String calculationHash;

    private LoanProductType loanProductType = LoanProductType.MORTGAGE;
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
    private com.surprise.mortgagesurprise.model.enums.Currency currency = com.surprise.mortgagesurprise.model.enums.Currency.RUB;
    private List<PaymentScheduleItem> items;
    private Instant calculatedAt;

    private Integer termDays;
    private BigDecimal dailyInterestRate;
    private LocalDate dueDate;
    private List<DailyScheduleItem> dailyItems;

    public PaymentScheduleDocument() {
    }

    public PaymentScheduleDocument(String id, String calculationHash, BigDecimal propertyPrice,
                                  BigDecimal downPayment, BigDecimal loanAmount, int termMonths,
                                  int actualTermMonths, BigDecimal annualInterestRate,
                                  PaymentType paymentType, BigDecimal monthlyPayment,
                                  BigDecimal totalInterest, BigDecimal totalPayment,
                                  List<PaymentScheduleItem> items, Instant calculatedAt) {
        this(id, calculationHash, propertyPrice, downPayment, loanAmount, termMonths, actualTermMonths,
                annualInterestRate, paymentType, com.surprise.mortgagesurprise.model.enums.Currency.RUB,
                monthlyPayment, totalInterest, totalPayment, items, calculatedAt);
    }

    public PaymentScheduleDocument(String id, String calculationHash, BigDecimal propertyPrice,
                                  BigDecimal downPayment, BigDecimal loanAmount, int termMonths,
                                  int actualTermMonths, BigDecimal annualInterestRate,
                                  PaymentType paymentType,
                                  com.surprise.mortgagesurprise.model.enums.Currency currency,
                                  BigDecimal monthlyPayment,
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
        this.currency = currency != null ? currency : com.surprise.mortgagesurprise.model.enums.Currency.RUB;
        this.monthlyPayment = monthlyPayment;
        this.totalInterest = totalInterest;
        this.totalPayment = totalPayment;
        this.items = items;
        this.calculatedAt = calculatedAt;
    }

    public PaymentScheduleDocument(String id, String calculationHash, BigDecimal loanAmount,
                                  int termDays, BigDecimal dailyInterestRate, BigDecimal annualInterestRate,
                                  BigDecimal totalInterest, BigDecimal totalPayment,
                                  LocalDate dueDate, com.surprise.mortgagesurprise.model.enums.Currency currency,
                                  List<DailyScheduleItem> dailyItems, Instant calculatedAt) {
        this.id = id;
        this.calculationHash = calculationHash;
        this.loanProductType = LoanProductType.MICROLOAN;
        this.loanAmount = loanAmount;
        this.termDays = termDays;
        this.dailyInterestRate = dailyInterestRate;
        this.annualInterestRate = annualInterestRate;
        this.totalInterest = totalInterest;
        this.totalPayment = totalPayment;
        this.dueDate = dueDate;
        this.currency = currency != null ? currency : com.surprise.mortgagesurprise.model.enums.Currency.RUB;
        this.dailyItems = dailyItems;
        this.calculatedAt = calculatedAt;
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

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public List<DailyScheduleItem> getDailyItems() {
        return dailyItems;
    }

    public void setDailyItems(List<DailyScheduleItem> dailyItems) {
        this.dailyItems = dailyItems;
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

    public com.surprise.mortgagesurprise.model.enums.Currency getCurrency() {
        return currency;
    }

    public void setCurrency(com.surprise.mortgagesurprise.model.enums.Currency currency) {
        this.currency = currency;
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
