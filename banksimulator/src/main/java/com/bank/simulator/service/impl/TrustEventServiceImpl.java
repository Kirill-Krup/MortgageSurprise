package com.bank.simulator.service.impl;

import com.bank.simulator.config.TrustProperties;
import com.bank.simulator.model.Subject;
import com.bank.simulator.model.SubjectDocument;
import com.bank.simulator.model.TrustEvent;
import com.bank.simulator.repository.SubjectRepository;
import com.bank.simulator.repository.TrustEventRepository;
import com.bank.simulator.service.TrustEventService;
import com.surprise.common.dto.simulator.DocumentType;
import com.surprise.common.dto.simulator.RegisterEventRequest;
import com.surprise.common.dto.simulator.TrustEventResponse;
import com.surprise.common.dto.simulator.TrustEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrustEventServiceImpl implements TrustEventService {

    private final SubjectRepository subjectRepository;
    private final TrustEventRepository trustEventRepository;
    private final TrustProperties trustProperties;

    @Override
    @Transactional
    public TrustEventResponse registerEvent(RegisterEventRequest request) {
        String eventKey = request.eventKey().trim();

        Optional<TrustEvent> existingEvent = trustEventRepository.findByEventKey(eventKey);
        if (existingEvent.isPresent()) {
            log.info("Event with key '{}' already processed. Returning idempotent cached result.", eventKey);
            return toResponse(existingEvent.get());
        }

        Subject subject = resolveOrCreateSubject(request.documentHash().trim());
        int delta = resolveDelta(request);
        int newScore = subject.applyScoreDelta(delta);

        applyBlockPolicy(subject, request.eventType());
        subjectRepository.save(subject);

        TrustEvent saved = saveTrustEvent(subject, request, eventKey, delta, newScore);
        log.info("Processed event '{}' for subject {}: delta={}, scoreAfter={}",
                eventKey, subject.getId(), delta, newScore);

        return toResponse(saved);
    }

    private Subject resolveOrCreateSubject(String documentHash) {
        return subjectRepository.findByDocumentHash(documentHash)
                .orElseGet(() -> createLazySubject(documentHash));
    }

    private Subject createLazySubject(String documentHash) {
        log.info("Subject not found for hash '{}'. Lazy-creating with initial score {}",
                documentHash, trustProperties.defaultInitialScore());

        Subject subject = Subject.builder()
                .trustScore(trustProperties.defaultInitialScore())
                .hardBlock(false)
                .build();

        SubjectDocument doc = SubjectDocument.builder()
                .documentType(DocumentType.PASSPORT)
                .documentHash(documentHash)
                .build();

        subject.addDocument(doc);
        return subjectRepository.save(subject);
    }

    private int resolveDelta(RegisterEventRequest request) {
        return request.delta() != null
                ? request.delta()
                : trustProperties.getDelta(request.eventType());
    }

    private void applyBlockPolicy(Subject subject, TrustEventType eventType) {
        if (eventType == null) {
            return;
        }
        switch (eventType) {
            case HARD_BLOCK_MANUAL, FRAUD_SUSPECTED, BANKRUPTCY -> {
                String reason = (subject.getBlockReason() == null || subject.getBlockReason().isBlank())
                        ? eventType.name()
                        : subject.getBlockReason();
                subject.applyHardBlock(reason);
            }
            case HARD_UNBLOCK_MANUAL -> subject.clearHardBlock();
            default -> {
                // Other event types only affect trustScore via configured delta.
            }
        }
    }

    private TrustEvent saveTrustEvent(
            Subject subject,
            RegisterEventRequest request,
            String eventKey,
            int delta,
            int scoreAfter
    ) {
        TrustEvent trustEvent = TrustEvent.builder()
                .subject(subject)
                .eventKey(eventKey)
                .eventType(request.eventType())
                .delta(delta)
                .scoreAfter(scoreAfter)
                .source(request.source().trim())
                .details(request.details())
                .occurredAt(request.occurredAt() != null ? request.occurredAt() : Instant.now())
                .build();
        return trustEventRepository.save(trustEvent);
    }

    private TrustEventResponse toResponse(TrustEvent event) {
        return TrustEventResponse.builder()
                .id(event.getId())
                .subjectId(event.getSubject().getId())
                .eventKey(event.getEventKey())
                .eventType(event.getEventType())
                .delta(event.getDelta())
                .scoreAfter(event.getScoreAfter())
                .source(event.getSource())
                .occurredAt(event.getOccurredAt())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
