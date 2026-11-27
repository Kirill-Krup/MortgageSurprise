package com.idfinance.credit_scoring_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "country_rules")
@SoftDelete(columnName = "is_active", strategy = SoftDeleteType.ACTIVE)
public class CountryRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "country_code")
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type")
    private RuleType ruleType;

    @Column(name = "credit_scoring")
    private Float creditScore;

    public CountryRule(String countryCode, RuleType ruleType, Float creditScore) {
        this.countryCode = countryCode;
        this.ruleType = ruleType;
        this.creditScore = creditScore;
    }
}