package com.idfinance.credit_scoring_service.repository;

import com.idfinance.credit_scoring_service.model.CountryRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CountryRuleRepository extends JpaRepository<CountryRule, Integer> {
    List<Integer> findAllIds();
}