package com.bank.simulator.service.impl;

import com.bank.simulator.config.TrustProperties;
import com.bank.simulator.dto.AddDocumentRequest;
import com.bank.simulator.dto.HardBlockRequest;
import com.bank.simulator.mapper.SubjectMapper;
import com.bank.simulator.model.Subject;
import com.bank.simulator.model.SubjectDocument;
import com.bank.simulator.repository.SubjectDocumentRepository;
import com.bank.simulator.repository.SubjectRepository;
import com.bank.simulator.service.SubjectService;
import com.surprise.common.dto.simulator.CreateSubjectRequest;
import com.surprise.common.dto.simulator.SubjectResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectServiceImpl implements SubjectService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SubjectRepository subjectRepository;
    private final SubjectDocumentRepository documentRepository;
    private final TrustProperties trustProperties;
    private final SubjectMapper subjectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getAll(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return subjectRepository.findAll(pageRequest)
                .map(subjectMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getById(UUID id) {
        return subjectMapper.toResponse(findSubjectOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getByDocumentHash(String documentHash) {
        if (documentHash == null || documentHash.isBlank()) {
            throw new IllegalArgumentException("Document hash must not be blank");
        }
        String normalizedHash = documentHash.trim();
        return subjectRepository.findByDocumentHash(normalizedHash)
                .map(subjectMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Subject not found by document hash: " + normalizedHash));
    }

    @Override
    @Transactional
    public SubjectResponse createSubject(CreateSubjectRequest request) {
        String hash = request.documentHash().trim();
        ensureDocumentHashAvailable(hash, "Document with hash '" + hash + "' is already registered");

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

        return subjectMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SubjectResponse addDocument(UUID subjectId, AddDocumentRequest request) {
        Subject subject = findSubjectOrThrow(subjectId);

        String hash = request.documentHash().trim();
        ensureDocumentHashAvailable(hash, "Document with hash '" + hash + "' is already linked to a subject");

        SubjectDocument doc = SubjectDocument.builder()
                .documentType(request.documentType())
                .documentHash(hash)
                .build();

        subject.addDocument(doc);
        Subject saved = subjectRepository.save(subject);
        log.info("Linked document {} ({}) to subject {}", hash, request.documentType(), subjectId);

        return subjectMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SubjectResponse hardBlock(UUID subjectId, HardBlockRequest request) {
        Subject subject = findSubjectOrThrow(subjectId);
        subject.applyHardBlock(request.reason().trim());
        Subject saved = subjectRepository.save(subject);
        log.warn("Applied hard block to subject {}: {}", subjectId, request.reason());
        return subjectMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SubjectResponse unblock(UUID subjectId) {
        Subject subject = findSubjectOrThrow(subjectId);
        subject.clearHardBlock();
        Subject saved = subjectRepository.save(subject);
        log.info("Removed hard block from subject {}", subjectId);

        return subjectMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new EntityNotFoundException("Subject not found: " + subjectId);
        }
        subjectRepository.deleteById(subjectId);
        log.info("Deleted subject {}", subjectId);
    }

    private Subject findSubjectOrThrow(UUID subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new EntityNotFoundException("Subject not found by id: " + subjectId));
    }

    private void ensureDocumentHashAvailable(String hash, String errorMessage) {
        if (documentRepository.existsByDocumentHash(hash)) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
