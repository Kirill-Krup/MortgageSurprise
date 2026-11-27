package com.surprise.mortgagesurprise.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "microloan_rates")
@NoArgsConstructor
public class MicroloanRate extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "daily_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal baseDailyRate = new BigDecimal("0.8000");

    @Column(name = "min_day_required")
    private Integer minDayRequired = 0;

    @Column(name = "max_day_required")
    private Integer maxDayRequired = Integer.MAX_VALUE;

    public MicroloanRate(String name, BigDecimal baseDailyRate, Integer minDayRequired,
                         Integer maxDayRequired) {
        this.name = name;
        this.baseDailyRate = baseDailyRate;
        this.minDayRequired = minDayRequired;
        this.maxDayRequired = maxDayRequired;
    }
}