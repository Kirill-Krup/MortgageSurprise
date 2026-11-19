package com.surprise.mortgagesurprise.repository.jpa;

import com.surprise.mortgagesurprise.model.entity.UserAbTestVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAbTestVariantRepository extends JpaRepository<UserAbTestVariant, UUID> {

    Optional<UserAbTestVariant> findByUserIdAndExperimentName(UUID userId, String experimentName);

    List<UserAbTestVariant> findByExperimentName(String experimentName);

    List<UserAbTestVariant> findByUserId(UUID userId);
}
