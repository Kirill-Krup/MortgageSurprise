package com.idfinance.credit_scoring_service.repository;

import com.idfinance.credit_scoring_service.model.AgeRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgeRuleRepository extends JpaRepository<AgeRule, Integer> {
}