package com.bank.simulator.service;

import com.bank.simulator.config.TrustProperties;
import com.surprise.common.dto.simulator.RegisterEventRequest;
import com.surprise.common.dto.simulator.TrustEventResponse;
import com.bank.simulator.model.Subject;
import com.bank.simulator.model.TrustEvent;
import com.bank.simulator.repository.SubjectRepository;
import com.bank.simulator.repository.TrustEventRepository;
import com.bank.simulator.service.impl.TrustEventServiceImpl;
import com.surprise.common.dto.simulator.TrustEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrustEventService Unit Tests")
class TrustEventServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TrustEventRepository trustEventRepository;

    private TrustEventService trustEventService;

    @BeforeEach
    void setUp() {
        TrustProperties properties = new TrustProperties(
                300,
                700,
                "secret",
                Map.of(
                        TrustEventType.PAYMENT_ON_TIME, 15,
                        TrustEventType.OVERDUE_90_DAYS, -200
                )
        );
        trustEventService = new TrustEventServiceImpl(subjectRepository, trustEventRepository, properties);
    }

    @Test
    @DisplayName("Returns cached event response idempotently when eventKey already exists")
    void registerEvent_DuplicateEventKey_ReturnsIdempotentResult() {
        UUID subjectId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Subject subject = Subject.builder().id(subjectId).trustScore(715).build();
        TrustEvent existing = TrustEvent.builder()
                .id(eventId)
                .subject(subject)
                .eventKey("evt-1")
                .eventType(TrustEventType.PAYMENT_ON_TIME)
                .delta(15)
                .scoreAfter(715)
                .source("core")
                .occurredAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();

        when(trustEventRepository.findByEventKey("evt-1")).thenReturn(Optional.of(existing));

        TrustEventResponse response = trustEventService.registerEvent(
                RegisterEventRequest.builder()
                        .eventKey("  evt-1  ")
                        .documentHash("hash-1")
                        .eventType(TrustEventType.PAYMENT_ON_TIME)
                        .source("core")
                        .build()
        );

        assertThat(response.id()).isEqualTo(eventId);
        assertThat(response.subjectId()).isEqualTo(subjectId);
        assertThat(response.scoreAfter()).isEqualTo(715);
        verify(subjectRepository, never()).save(any());
        verify(trustEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Updates existing subject score using configured rule delta")
    void registerEvent_ExistingSubject_AppliesConfiguredDelta() {
        UUID subjectId = UUID.randomUUID();
        Subject subject = Subject.builder().id(subjectId).trustScore(700).build();

        when(trustEventRepository.findByEventKey("evt-2")).thenReturn(Optional.empty());
        when(subjectRepository.findByDocumentHash("hash-2")).thenReturn(Optional.of(subject));
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(trustEventRepository.save(any(TrustEvent.class))).thenAnswer(inv -> {
            TrustEvent e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        TrustEventResponse response = trustEventService.registerEvent(
                RegisterEventRequest.builder()
                        .eventKey("evt-2")
                        .documentHash("  hash-2 ")
                        .eventType(TrustEventType.PAYMENT_ON_TIME)
                        .source("  scoring-service ")
                        .occurredAt(Instant.parse("2026-02-01T12:00:00Z"))
                        .build()
        );

        assertThat(response.delta()).isEqualTo(15);
        assertThat(response.scoreAfter()).isEqualTo(715);
        assertThat(response.source()).isEqualTo("scoring-service");
        assertThat(subject.getTrustScore()).isEqualTo(715);
    }

    @Test
    @DisplayName("Lazy-creates subject with default initial score when document hash is unknown")
    void registerEvent_UnknownSubject_LazyCreatesSubject() {
        when(trustEventRepository.findByEventKey("evt-3")).thenReturn(Optional.empty());
        when(subjectRepository.findByDocumentHash("new-hash")).thenReturn(Optional.empty());
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> {
            Subject s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            return s;
        });
        when(trustEventRepository.save(any(TrustEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        TrustEventResponse response = trustEventService.registerEvent(
                RegisterEventRequest.builder()
                        .eventKey("evt-3")
                        .documentHash("new-hash")
                        .eventType(TrustEventType.OVERDUE_90_DAYS)
                        .source("bank")
                        .build()
        );

        assertThat(response.delta()).isEqualTo(-200);
        assertThat(response.scoreAfter()).isEqualTo(500);
        assertThat(response.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("Clamps trust score within [0, 1000] bounds when custom delta exceeds limits")
    void registerEvent_CustomDeltaExceedingBounds_ClampsScore() {
        Subject highSubject = Subject.builder().id(UUID.randomUUID()).trustScore(950).build();
        Subject lowSubject = Subject.builder().id(UUID.randomUUID()).trustScore(50).build();

        when(trustEventRepository.findByEventKey(any())).thenReturn(Optional.empty());
        when(subjectRepository.findByDocumentHash("high")).thenReturn(Optional.of(highSubject));
        when(subjectRepository.findByDocumentHash("low")).thenReturn(Optional.of(lowSubject));
        when(subjectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(trustEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TrustEventResponse upper = trustEventService.registerEvent(
                RegisterEventRequest.builder()
                        .eventKey("evt-high")
                        .documentHash("high")
                        .eventType(TrustEventType.LOAN_CLOSED)
                        .delta(500)
                        .source("bank")
                        .build()
        );
        TrustEventResponse lower = trustEventService.registerEvent(
                RegisterEventRequest.builder()
                        .eventKey("evt-low")
                        .documentHash("low")
                        .eventType(TrustEventType.OVERDUE_90_DAYS)
                        .delta(-500)
                        .source("bank")
                        .build()
        );

        assertThat(upper.scoreAfter()).isEqualTo(1000);
        assertThat(lower.scoreAfter()).isEqualTo(0);
    }

    @Test
    @DisplayName("Applies hard block on FRAUD_SUSPECTED and removes it on HARD_UNBLOCK_MANUAL")
    void registerEvent_BlockAndUnblockEvents_UpdatesHardBlockStatus() {
        Subject subject = Subject.builder().id(UUID.randomUUID()).trustScore(700).hardBlock(false).build();

        when(trustEventRepository.findByEventKey(any())).thenReturn(Optional.empty());
        when(subjectRepository.findByDocumentHash("hash-blk")).thenReturn(Optional.of(subject));
        when(subjectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(trustEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        trustEventService.registerEvent(
                RegisterEventRequest.builder()
                        .eventKey("evt-fraud")
                        .documentHash("hash-blk")
                        .eventType(TrustEventType.FRAUD_SUSPECTED)
                        .source("antifraud")
                        .build()
        );

        assertThat(subject.isHardBlock()).isTrue();
        assertThat(subject.getBlockReason()).isEqualTo("FRAUD_SUSPECTED");

        trustEventService.registerEvent(
                RegisterEventRequest.builder()
                        .eventKey("evt-unblock")
                        .documentHash("hash-blk")
                        .eventType(TrustEventType.HARD_UNBLOCK_MANUAL)
                        .source("admin")
                        .build()
        );

        assertThat(subject.isHardBlock()).isFalse();
        assertThat(subject.getBlockReason()).isNull();
    }
}
