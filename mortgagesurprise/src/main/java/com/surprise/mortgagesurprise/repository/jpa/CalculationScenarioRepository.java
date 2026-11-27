package com.surprise.mortgagesurprise.repository.jpa;

import com.surprise.mortgagesurprise.model.entity.CalculationScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface CalculationScenarioRepository extends JpaRepository<CalculationScenario, UUID> {
    List<CalculationScenario> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<CalculationScenario> findAllByIdInAndUserId(List<UUID> ids, UUID userId);
    List<CalculationScenario> findByAnnualInterestRate(BigDecimal annualInterestRate);
}
