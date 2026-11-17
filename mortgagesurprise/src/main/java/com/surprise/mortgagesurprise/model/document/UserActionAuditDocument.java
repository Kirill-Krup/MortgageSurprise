package com.surprise.mortgagesurprise.model.document;

import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Document(collection = "user_audit_logs")
@Getter
@Setter
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
}
