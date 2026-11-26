package com.bank.simulator.service;

import com.bank.simulator.config.TrustProperties;
import com.surprise.common.dto.simulator.RegisterEventRequest;
import com.surprise.common.dto.simulator.TrustEventResponse;
import com.bank.simulator.model.Subject;
import com.bank.simulator.model.SubjectDocument;
import com.bank.simulator.model.TrustEvent;
import com.surprise.common.dto.simulator.TrustEventType;
import com.bank.simulator.repository.SubjectRepository;
import com.bank.simulator.repository.TrustEventRepository;
import com.surprise.common.dto.simulator.DocumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrustEventService {

    private final SubjectRepository subjectRepository;
    private final TrustEventRepository trustEventRepository;
    private final TrustProperties trustProperties;

    @Transactional
    public TrustEventResponse registerEvent(RegisterEventRequest request) {
        String eventKey = request.eventKey().trim();

        Optional<TrustEvent> existingEventOpt = trustEventRepository.findByEventKey(eventKey);
        if (existingEventOpt.isPresent()) {
            log.info("Event with key '{}' already processed. Returning idempotent cached result.", eventKey);
            return toResponse(existingEventOpt.get());
        }

        String docHash = request.documentHash().trim();
        Subject subject = subjectRepository.findByDocumentHash(docHash)
                .orElseGet(() -> createLazySubject(docHash));

        int delta = request.delta() != null
                ? request.delta() 
                : trustProperties.getDelta(request.eventType());

        int currentScore = subject.getTrustScore();
        int newScore = Math.clamp((long) currentScore + delta, 0, 1000);
        subject.setTrustScore(newScore);

        handleBlockStatusByEventType(subject, request.eventType());

        subjectRepository.save(subject);

        TrustEvent trustEvent = TrustEvent.builder()
                .subject(subject)
                .eventKey(eventKey)
                .eventType(request.eventType())
                .delta(delta)
                .scoreAfter(newScore)
                .source(request.source().trim())
                .details(request.details())
                .occurredAt(request.occurredAt() != null ? request.occurredAt() : Instant.now())
                .build();

        TrustEvent saved = trustEventRepository.save(trustEvent);
        log.info("Processed event '{}' for subject {}: delta={}, scoreAfter={}",
                eventKey, subject.getId(), delta, newScore);

        return toResponse(saved);
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

    private void handleBlockStatusByEventType(Subject subject, TrustEventType eventType) {
        if (eventType == TrustEventType.HARD_BLOCK_MANUAL
                || eventType == TrustEventType.FRAUD_SUSPECTED
                || eventType == TrustEventType.BANKRUPTCY) {
            subject.setHardBlock(true);
            if (subject.getBlockReason() == null || subject.getBlockReason().isBlank()) {
                subject.setBlockReason(eventType.name());
            }
        } else if (eventType == TrustEventType.HARD_UNBLOCK_MANUAL) {
            subject.setHardBlock(false);
            subject.setBlockReason(null);
        }
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
