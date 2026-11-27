package com.surprise.mortgagesurprise.repository.jpa;

import com.surprise.mortgagesurprise.model.entity.MicroloanRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MicroloanRateRepository extends JpaRepository<MicroloanRate, Integer> {
    Optional<MicroloanRate> findByName(String name);
}
