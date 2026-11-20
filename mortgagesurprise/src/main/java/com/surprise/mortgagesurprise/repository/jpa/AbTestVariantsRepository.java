package com.surprise.mortgagesurprise.repository.jpa;

import com.surprise.mortgagesurprise.model.entity.AbTestVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface AbTestVariantsRepository extends JpaRepository<AbTestVariant, Integer> {
    Set<AbTestVariant> saveAll(Set<AbTestVariant> abTestVariants);
    Set<AbTestVariant> findAllByExperimentNameAndVersion(String experimentName, Integer version);
}
