package com.idfinance.credit_scoring_service.repository;

import com.idfinance.credit_scoring_service.model.GenderRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenderRuleRepository extends JpaRepository<GenderRule, Integer> {
    @Query("select gr.id from GenderRule gr")
    List<Integer> findAllIds();
}