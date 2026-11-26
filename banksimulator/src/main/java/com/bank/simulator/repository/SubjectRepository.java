package com.bank.simulator.repository;

import com.bank.simulator.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    @Query("SELECT s FROM Subject s JOIN s.documents d WHERE d.documentHash = :hash")
    Optional<Subject> findByDocumentHash(@Param("hash") String hash);
}
