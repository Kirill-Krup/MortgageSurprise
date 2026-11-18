package com.surprise.mortgagesurprise.repository.jpa;

import com.surprise.mortgagesurprise.model.entity.ExchangeRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, UUID> {

    boolean existsByUpdatedAtAfter(Instant threshold);

    @Query(value = """
        SELECT DISTINCT ON (target_currency) *
        FROM exchange_rates
        ORDER BY target_currency, updated_at DESC
        """, nativeQuery = true)
    List<ExchangeRateEntity> findLatestRatesDistinct();
}