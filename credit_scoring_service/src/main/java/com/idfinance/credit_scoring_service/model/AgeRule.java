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
@Table(name = "age_rules")
@SoftDelete(columnName = "is_active", strategy = SoftDeleteType.ACTIVE)
public class AgeRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type")
    private RuleType ruleType;

    @Column(name = "credit_scoring")
    private Float creditScore;

    public AgeRule(Integer minAge, Integer maxAge, RuleType ruleType, Float creditScore) {
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.ruleType = ruleType;
        this.creditScore = creditScore;
    }
}