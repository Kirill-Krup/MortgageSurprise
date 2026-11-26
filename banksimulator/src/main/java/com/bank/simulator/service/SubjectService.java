package com.bank.simulator.service;

import com.bank.simulator.config.TrustProperties;
import com.bank.simulator.dto.*;
import com.surprise.common.dto.simulator.*;
import com.bank.simulator.model.Subject;
import com.bank.simulator.model.SubjectDocument;
import com.bank.simulator.repository.SubjectDocumentRepository;
import com.bank.simulator.repository.SubjectRepository;
import com.surprise.common.dto.simulator.RiskLevel;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final SubjectDocumentRepository documentRepository;
    private final TrustProperties trustProperties;

    @Transactional(readOnly = true)
    public List<SubjectResponse> getAll(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return subjectRepository.findAll(pageRequest)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubjectResponse getById(UUID id) {
        return subjectRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Subject not found by id: " + id));
    }

    @Transactional(readOnly = true)
    public SubjectResponse getByDocumentHash(String documentHash) {
        return subjectRepository.findByDocumentHash(documentHash.trim())
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Subject not found by document hash: " + documentHash));
    }

    @Transactional
    public SubjectResponse createSubject(CreateSubjectRequest request) {
        String hash = request.documentHash().trim();
        if (documentRepository.existsByDocumentHash(hash)) {
            throw new IllegalArgumentException("Document with hash '" + hash + "' is already registered");
        }

        int initialScore = request.initialScore() != null 
                ? request.initialScore() 
                : trustProperties.defaultInitialScore();

        Subject subject = Subject.builder()
                .trustScore(initialScore)
                .hardBlock(false)
                .build();

        SubjectDocument doc = SubjectDocument.builder()
                .documentType(request.documentType())
                .documentHash(hash)
                .build();

        subject.addDocument(doc);
        Subject saved = subjectRepository.save(subject);
        log.info("Created subject {} with document hash {}", saved.getId(), hash);

        return toResponse(saved);
    }

    @Transactional
    public SubjectResponse addDocument(UUID subjectId, AddDocumentRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new EntityNotFoundException("Subject not found: " + subjectId));

        String hash = request.documentHash().trim();
        if (documentRepository.existsByDocumentHash(hash)) {
            throw new IllegalArgumentException("Document with hash '" + hash + "' is already linked to a subject");
        }

        SubjectDocument doc = SubjectDocument.builder()
                .documentType(request.documentType())
                .documentHash(hash)
                .build();

        subject.addDocument(doc);
        Subject saved = subjectRepository.save(subject);
        log.info("Linked document {} ({}) to subject {}", hash, request.documentType(), subjectId);

        return toResponse(saved);
    }

    @Transactional
    public SubjectResponse hardBlock(UUID subjectId, HardBlockRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new EntityNotFoundException("Subject not found: " + subjectId));

        subject.setHardBlock(true);
        subject.setBlockReason(request.reason().trim());
        Subject saved = subjectRepository.save(subject);
        log.warn("Applied hard block to subject {}: {}", subjectId, request.reason());

        return toResponse(saved);
    }

    @Transactional
    public SubjectResponse unblock(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new EntityNotFoundException("Subject not found: " + subjectId));

        subject.setHardBlock(false);
        subject.setBlockReason(null);
        Subject saved = subjectRepository.save(subject);
        log.info("Removed hard block from subject {}", subjectId);

        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new EntityNotFoundException("Subject not found: " + subjectId);
        }
        subjectRepository.deleteById(subjectId);
        log.info("Deleted subject {}", subjectId);
    }

    public SubjectResponse toResponse(Subject subject) {
        int score = subject.getTrustScore();
        boolean hardBlock = subject.isHardBlock();
        boolean blacklisted = hardBlock || score < trustProperties.blacklistThreshold();
        RiskLevel riskLevel = BlacklistCheckService.calculateRiskLevel(score, hardBlock);

        List<DocumentDto> docs = subject.getDocuments() != null
                ? subject.getDocuments().stream()
                    .map(d -> DocumentDto.builder()
                            .id(d.getId())
                            .documentType(d.getDocumentType())
                            .documentHash(d.getDocumentHash())
                            .createdAt(d.getCreatedAt())
                            .build())
                    .toList()
                : Collections.emptyList();

        return SubjectResponse.builder()
                .id(subject.getId())
                .trustScore(score)
                .hardBlock(hardBlock)
                .blockReason(subject.getBlockReason())
                .riskLevel(riskLevel)
                .blacklisted(blacklisted)
                .documents(docs)
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .build();
    }
}
