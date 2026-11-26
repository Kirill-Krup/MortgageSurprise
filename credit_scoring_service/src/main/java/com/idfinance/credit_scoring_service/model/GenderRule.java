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
@Table(name = "gender_rules")
@SoftDelete(columnName = "is_active", strategy = SoftDeleteType.ACTIVE)
public class GenderRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type")
    private RuleType ruleType;

    @Column(name = "credit_scoring")
    private Float creditScore;

    public GenderRule(String name, RuleType ruleType, Float creditScore) {
        this.name = name;
        this.ruleType = ruleType;
        this.creditScore = creditScore;
    }
}