package com.idfinance.credit_scoring_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;

@Entity
@Getter
@Setter
@Table(name = "city_rules")
@SoftDelete(columnName = "is_active")
public class CityRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_name")
    private String cityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type")
    private RuleType ruleType;

    @Column(name = "credit_score")
    private Float creditScore;
}