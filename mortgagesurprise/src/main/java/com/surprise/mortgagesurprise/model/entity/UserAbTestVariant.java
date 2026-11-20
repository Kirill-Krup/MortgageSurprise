package com.surprise.mortgagesurprise.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AbTestVariant abTestVariant;
}