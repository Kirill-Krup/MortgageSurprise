package com.surprise.mortgagesurprise.repository.jpa;

import com.surprise.mortgagesurprise.model.entity.UserPassportData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPassportDataRepository extends JpaRepository<UserPassportData, UUID> {
    Optional<UserPassportData> findByUserId(UUID userId);
    Optional<UserPassportData> findByPassportHash(String passportHash);
    boolean existsByPassportHashAndUserIdNot(String passportHash, UUID userId);
    void deleteByUserId(UUID userId);
}
