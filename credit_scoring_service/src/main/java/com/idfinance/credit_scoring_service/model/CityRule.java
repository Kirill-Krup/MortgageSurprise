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
@Table(name = "city_rules")
@SoftDelete(columnName = "is_active", strategy = SoftDeleteType.ACTIVE)
public class CityRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_name")
    private String cityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type")
    private RuleType ruleType;

    @Column(name = "credit_scoring")
    private Float creditScore;

    public CityRule(String cityName, RuleType ruleType, Float creditScore) {
        this.cityName = cityName;
        this.ruleType = ruleType;
        this.creditScore = creditScore;
    }
}