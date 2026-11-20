package com.surprise.mortgagesurprise.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "ab_test_variants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_ab_experiment", columnNames = {"user_id", "experiment_name"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AbTestVariant extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "experiment_name", nullable = false, length = 100)
    private String experimentName;

    @Column(name = "variant_name", nullable = false, length = 50)
    private String variantName;

    @Column(name = "variant_name", nullable = false)
    private Integer variantProbability = 0;

    @Column(name = "version")
    private Integer version = 0;

    @OneToMany(mappedBy = "abTestVariant", fetch = FetchType.LAZY)
    private Set<UserAbTestVariant> userAbTestVariants = new HashSet<>();

    public AbTestVariant(String experimentName, String variantName, Integer variantProbability) {
        this.experimentName = experimentName;
        this.variantName = variantName;
        this.variantProbability = variantProbability;
    }
}
