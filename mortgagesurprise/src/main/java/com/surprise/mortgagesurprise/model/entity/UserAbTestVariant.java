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
public class UserAbTestVariant extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "experiment_name", nullable = false, length = 100)
    private String experimentName;

    // No cascade: deleting an assignment must never delete the variant definition.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ab_test_variant_id", nullable = false)
    private AbTestVariant abTestVariant;

    public UserAbTestVariant(UUID userId, AbTestVariant abTestVariant) {
        this.userId = userId;
        this.abTestVariant = abTestVariant;
        this.experimentName = abTestVariant.getExperimentName();
    }
}
