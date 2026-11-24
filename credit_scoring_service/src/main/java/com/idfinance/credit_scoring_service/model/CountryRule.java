package com.idfinance.credit_scoring_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;

@Entity
@Getter
@Setter
@Table(name = "country_rules")
@SoftDelete(columnName = "is_active")
public class CountryRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "country_code")
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type")
    private RuleType ruleType;

    @Column(name = "credit_score")
    private Float creditScore;
}