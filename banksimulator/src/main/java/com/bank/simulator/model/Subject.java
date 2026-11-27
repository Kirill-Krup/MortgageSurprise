package com.bank.simulator.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "subject")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Subject {

    public static final int MIN_TRUST_SCORE = 0;
    public static final int MAX_TRUST_SCORE = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Builder.Default
    @Column(name = "trust_score", nullable = false)
    private int trustScore = 700;

    @Builder.Default
    @Column(name = "hard_block", nullable = false)
    private boolean hardBlock = false;

    @Column(name = "block_reason")
    private String blockReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @BatchSize(size = 50)
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubjectDocument> documents = new ArrayList<>();

    public void addDocument(SubjectDocument document) {
        documents.add(document);
        document.setSubject(this);
    }

    public void removeDocument(SubjectDocument document) {
        documents.remove(document);
        document.setSubject(null);
    }

    public int applyScoreDelta(int delta) {
        this.trustScore = Math.clamp((long) this.trustScore + delta, MIN_TRUST_SCORE, MAX_TRUST_SCORE);
        return this.trustScore;
    }

    public void applyHardBlock(String reason) {
        this.hardBlock = true;
        this.blockReason = reason;
    }

    public void clearHardBlock() {
        this.hardBlock = false;
        this.blockReason = null;
    }

    public boolean isBlacklisted(int blacklistThreshold) {
        return this.hardBlock || this.trustScore < blacklistThreshold;
    }
}
