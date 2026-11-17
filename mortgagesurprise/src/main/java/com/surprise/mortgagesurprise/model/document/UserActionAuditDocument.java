package com.surprise.mortgagesurprise.model.document;

import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Document(collection = "user_audit_logs")
public class UserActionAuditDocument {

    @Id
    private String id;

    @Indexed
    private UUID userId;

    @Indexed
    private String userEmail;

    @Indexed
    private UserAuditActionType actionType;

    private String actionDescription;

    private Long bankOfferId;
    private String bankName;
    private String offerName;

    private BigDecimal loanAmount;
    private BigDecimal propertyPrice;
    private BigDecimal downPayment;
    private Integer termMonths;
    private Integer termDays;
    private BigDecimal interestRate;
    private BigDecimal monthlyPayment;
    private BigDecimal totalPayment;
    private BigDecimal totalInterest;

    private String microloanType;
    private String loanPurpose;
    private String applicationId;
    private Currency currency;

    private Map<String, Object> metadata;

    @Indexed
    private Instant timestamp;

    public UserActionAuditDocument() {
    }

    public UserActionAuditDocument(String id, UUID userId, String userEmail, UserAuditActionType actionType,
                                   String actionDescription, Long bankOfferId, String bankName, String offerName,
                                   BigDecimal loanAmount, BigDecimal propertyPrice, BigDecimal downPayment,
                                   Integer termMonths, Integer termDays, BigDecimal interestRate,
                                   BigDecimal monthlyPayment, BigDecimal totalPayment, BigDecimal totalInterest,
                                   String microloanType, String loanPurpose, String applicationId,
                                   Currency currency, Map<String, Object> metadata, Instant timestamp) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.actionType = actionType;
        this.actionDescription = actionDescription;
        this.bankOfferId = bankOfferId;
        this.bankName = bankName;
        this.offerName = offerName;
        this.loanAmount = loanAmount;
        this.propertyPrice = propertyPrice;
        this.downPayment = downPayment;
        this.termMonths = termMonths;
        this.termDays = termDays;
        this.interestRate = interestRate;
        this.monthlyPayment = monthlyPayment;
        this.totalPayment = totalPayment;
        this.totalInterest = totalInterest;
        this.microloanType = microloanType;
        this.loanPurpose = loanPurpose;
        this.applicationId = applicationId;
        this.currency = currency;
        this.metadata = metadata;
        this.timestamp = timestamp;
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

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public UserAuditActionType getActionType() {
        return actionType;
    }

    public void setActionType(UserAuditActionType actionType) {
        this.actionType = actionType;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    }

    public Long getBankOfferId() {
        return bankOfferId;
    }

    public void setBankOfferId(Long bankOfferId) {
        this.bankOfferId = bankOfferId;
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

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
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

    public Integer getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(Integer termMonths) {
        this.termMonths = termMonths;
    }

    public Integer getTermDays() {
        return termDays;
    }

    public void setTermDays(Integer termDays) {
        this.termDays = termDays;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(BigDecimal monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public BigDecimal getTotalPayment() {
        return totalPayment;
    }

    public void setTotalPayment(BigDecimal totalPayment) {
        this.totalPayment = totalPayment;
    }

    public BigDecimal getTotalInterest() {
        return totalInterest;
    }

    public void setTotalInterest(BigDecimal totalInterest) {
        this.totalInterest = totalInterest;
    }

    public String getMicroloanType() {
        return microloanType;
    }

    public void setMicroloanType(String microloanType) {
        this.microloanType = microloanType;
    }

    public String getLoanPurpose() {
        return loanPurpose;
    }

    public void setLoanPurpose(String loanPurpose) {
        this.loanPurpose = loanPurpose;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
