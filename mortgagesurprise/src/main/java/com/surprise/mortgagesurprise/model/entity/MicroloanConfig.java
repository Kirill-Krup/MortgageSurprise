package com.surprise.mortgagesurprise.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "microloan_configs")
public class MicroloanConfig extends AuditableEntity {

    @Id
    private String id = "DEFAULT";

    @Column(name = "bank_interest", nullable = false, precision = 10, scale = 4)
    private BigDecimal bankInterest = new BigDecimal("2000");

    public MicroloanConfig(String id, BigDecimal bankInterest) {
        this.id = id;
        this.bankInterest = bankInterest;
    }
}