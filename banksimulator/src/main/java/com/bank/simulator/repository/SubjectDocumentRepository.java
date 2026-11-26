package com.bank.simulator.repository;

import com.bank.simulator.model.SubjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectDocumentRepository extends JpaRepository<SubjectDocument, UUID> {

    Optional<SubjectDocument> findByDocumentHash(String documentHash);

    boolean existsByDocumentHash(String documentHash);

    List<SubjectDocument> findBySubjectId(UUID subjectId);
}
