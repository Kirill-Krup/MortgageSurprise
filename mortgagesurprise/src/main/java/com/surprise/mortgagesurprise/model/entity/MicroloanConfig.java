package com.surprise.mortgagesurprise.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "microloan_configs")
@Getter
@Setter
public class MicroloanConfig {

    @Id
    private String id = "DEFAULT";

    @Column(name = "base_daily_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal baseDailyRate = new BigDecimal("0.8000");

    @Column(name = "vanity_surcharge_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal vanitySurchargeRate = new BigDecimal("5.0000");

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public MicroloanConfig() {}

    public MicroloanConfig(String id, BigDecimal baseDailyRate, BigDecimal vanitySurchargeRate, Instant updatedAt) {
        this.id = id;
        this.baseDailyRate = baseDailyRate;
        this.vanitySurchargeRate = vanitySurchargeRate;
        this.updatedAt = updatedAt;
    }
}
