package com.surprise.mortgagesurprise.model.document;

import com.surprise.mortgagesurprise.model.enums.LoanProductType;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Document(collection = "calculation_history")
@Getter
@Setter
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
}
