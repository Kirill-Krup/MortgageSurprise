package com.surprise.mortgagesurprise.repository.jpa;

import com.surprise.mortgagesurprise.model.entity.AbTestVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface AbTestVariantsRepository extends JpaRepository<AbTestVariant, Integer> {

    Set<AbTestVariant> findAllByExperimentNameAndVersion(String experimentName, Integer version);

    /** The latest version of an experiment is the one used for new assignments. */
    @Query("select max(v.version) from AbTestVariant v where v.experimentName = :experimentName")
    Optional<Integer> findLatestVersion(@Param("experimentName") String experimentName);

    @Query("select distinct v.experimentName from AbTestVariant v")
    java.util.List<String> findDistinctExperimentNames();

    void deleteAllByExperimentName(String experimentName);
}
