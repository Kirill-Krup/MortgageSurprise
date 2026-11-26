package com.bank.simulator.repository;

import com.bank.simulator.model.TrustEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrustEventRepository extends JpaRepository<TrustEvent, UUID> {

    Optional<TrustEvent> findByEventKey(String eventKey);

    boolean existsByEventKey(String eventKey);

    List<TrustEvent> findBySubjectIdOrderByCreatedAtDesc(UUID subjectId);
}
