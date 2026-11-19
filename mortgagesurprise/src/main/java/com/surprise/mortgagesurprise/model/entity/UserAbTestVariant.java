package com.surprise.mortgagesurprise.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "user_ab_test_variants",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_ab_experiment", columnNames = {"user_id", "experiment_name"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class UserAbTestVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "experiment_name", nullable = false, length = 100)
    private String experimentName;

    @Column(name = "color", nullable = false, length = 50)
    private String color;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UserAbTestVariant(UUID userId, String experimentName, String color) {
        this.userId = userId;
        this.experimentName = experimentName;
        this.color = color;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
