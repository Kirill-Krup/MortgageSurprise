package com.surprise.mortgagesurprise.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "ab_test_variants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ab_variant", columnNames = {"experiment_name", "version", "variant_name"})
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

    @Column(name = "variant_probability", nullable = false)
    private Integer variantProbability = 0;

    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public AbTestVariant(String experimentName, String variantName, Integer variantProbability, Integer version) {
        this.experimentName = experimentName;
        this.variantName = variantName;
        this.variantProbability = variantProbability;
        this.version = version;
    }
}
