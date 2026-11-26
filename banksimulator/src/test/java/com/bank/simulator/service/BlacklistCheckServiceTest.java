package com.bank.simulator.service;

import com.bank.simulator.config.TrustProperties;
import com.bank.simulator.model.Subject;
import com.bank.simulator.repository.SubjectRepository;
import com.surprise.common.dto.simulator.BlacklistCheckRequest;
import com.surprise.common.dto.simulator.BlacklistCheckResponse;
import com.surprise.common.dto.simulator.DocumentType;
import com.surprise.common.dto.simulator.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlacklistCheckService Unit Tests")
class BlacklistCheckServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private DocumentHashService documentHashService;

    private TrustProperties trustProperties;
    private BlacklistCheckService blacklistCheckService;

    @BeforeEach
    void setUp() {
        trustProperties = new TrustProperties(300, 700, "secret", Map.of());
        blacklistCheckService = new BlacklistCheckService(subjectRepository, trustProperties, documentHashService);
    }

    @Test
    @DisplayName("Returns clean default response when subject is not found by hash")
    void check_SubjectNotFound_ReturnsDefaultCleanResponse() {
        String hash = "abc123";
        when(subjectRepository.findByDocumentHash(hash)).thenReturn(Optional.empty());

        BlacklistCheckResponse response = blacklistCheckService.check(
                BlacklistCheckRequest.builder()
                        .documentType(DocumentType.PASSPORT)
                        .documentHash("  " + hash + "  ")
                        .build()
        );

        assertThat(response.blacklisted()).isFalse();
        assertThat(response.trustScore()).isEqualTo(700);
        assertThat(response.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(response.hardBlock()).isFalse();
        assertThat(response.reasons()).isEmpty();
        verifyNoInteractions(documentHashService);
    }

    @Test
    @DisplayName("Hashes documentNumber when documentHash is absent")
    void check_DocumentNumberProvided_HashesAndChecksSubject() {
        String rawNumber = "4510 123456";
        String computedHash = "computed-hash";
        Subject subject = Subject.builder()
                .id(UUID.randomUUID())
                .trustScore(650)
                .hardBlock(false)
                .build();

        when(documentHashService.hash(rawNumber)).thenReturn(computedHash);
        when(subjectRepository.findByDocumentHash(computedHash)).thenReturn(Optional.of(subject));

        BlacklistCheckResponse response = blacklistCheckService.check(
                BlacklistCheckRequest.builder()
                        .documentType(DocumentType.PASSPORT)
                        .documentNumber(rawNumber)
                        .build()
        );

        assertThat(response.blacklisted()).isFalse();
        assertThat(response.trustScore()).isEqualTo(650);
        assertThat(response.riskLevel()).isEqualTo(RiskLevel.LOW);
        verify(documentHashService).hash(rawNumber);
    }

    @Test
    @DisplayName("Marks subject blacklisted with HIGH risk when score is below threshold")
    void check_ScoreBelowThreshold_ReturnsBlacklistedWithReason() {
        Subject subject = Subject.builder()
                .id(UUID.randomUUID())
                .trustScore(250)
                .hardBlock(false)
                .build();

        when(subjectRepository.findByDocumentHash("hash-1")).thenReturn(Optional.of(subject));

        BlacklistCheckResponse response = blacklistCheckService.check(
                BlacklistCheckRequest.builder().documentHash("hash-1").build()
        );

        assertThat(response.blacklisted()).isTrue();
        assertThat(response.hardBlock()).isFalse();
        assertThat(response.trustScore()).isEqualTo(250);
        assertThat(response.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(response.reasons()).containsExactly("SCORE_BELOW_THRESHOLD: 250 < 300");
    }

    @Test
    @DisplayName("Marks subject blacklisted with HIGH risk when hardBlock is true even with high score")
    void check_HardBlockApplied_ReturnsBlacklistedWithHardBlockReason() {
        Subject subject = Subject.builder()
                .id(UUID.randomUUID())
                .trustScore(800)
                .hardBlock(true)
                .blockReason("Fraud confirmed")
                .build();

        when(subjectRepository.findByDocumentHash("hash-2")).thenReturn(Optional.of(subject));

        BlacklistCheckResponse response = blacklistCheckService.check(
                BlacklistCheckRequest.builder().documentHash("hash-2").build()
        );

        assertThat(response.blacklisted()).isTrue();
        assertThat(response.hardBlock()).isTrue();
        assertThat(response.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(response.reasons()).containsExactly("HARD_BLOCK: Fraud confirmed");
    }

    @Test
    @DisplayName("Uses fallback hard block reason and includes both reasons when both conditions fail")
    void check_HardBlockWithoutReasonAndLowScore_IncludesBothReasons() {
        Subject subject = Subject.builder()
                .id(UUID.randomUUID())
                .trustScore(100)
                .hardBlock(true)
                .blockReason(null)
                .build();

        when(subjectRepository.findByDocumentHash("hash-3")).thenReturn(Optional.of(subject));

        BlacklistCheckResponse response = blacklistCheckService.check(
                BlacklistCheckRequest.builder().documentHash("hash-3").build()
        );

        assertThat(response.blacklisted()).isTrue();
        assertThat(response.reasons()).containsExactly(
                "HARD_BLOCK: Block applied",
                "SCORE_BELOW_THRESHOLD: 100 < 300"
        );
    }

    @Test
    @DisplayName("Returns MEDIUM risk when score is between 300 and 599 without hard block")
    void check_MediumScore_ReturnsMediumRiskNotBlacklisted() {
        Subject subject = Subject.builder()
                .id(UUID.randomUUID())
                .trustScore(450)
                .hardBlock(false)
                .build();

        when(subjectRepository.findByDocumentHash("hash-4")).thenReturn(Optional.of(subject));

        BlacklistCheckResponse response = blacklistCheckService.check(
                BlacklistCheckRequest.builder().documentHash("hash-4").build()
        );

        assertThat(response.blacklisted()).isFalse();
        assertThat(response.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(response.reasons()).isEmpty();
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when neither documentHash nor documentNumber is provided")
    void check_MissingHashAndNumber_ThrowsIllegalArgumentException() {
        BlacklistCheckRequest request = BlacklistCheckRequest.builder()
                .documentHash("   ")
                .documentNumber(null)
                .build();

        assertThatThrownBy(() -> blacklistCheckService.check(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either documentHash or documentNumber must be provided");
    }

    @Test
    @DisplayName("Calculates RiskLevel accurately across score boundaries and hardBlock flag")
    void calculateRiskLevel_BoundaryValues_ReturnsExpectedLevels() {
        assertThat(BlacklistCheckService.calculateRiskLevel(0, false)).isEqualTo(RiskLevel.HIGH);
        assertThat(BlacklistCheckService.calculateRiskLevel(299, false)).isEqualTo(RiskLevel.HIGH);
        assertThat(BlacklistCheckService.calculateRiskLevel(300, false)).isEqualTo(RiskLevel.MEDIUM);
        assertThat(BlacklistCheckService.calculateRiskLevel(599, false)).isEqualTo(RiskLevel.MEDIUM);
        assertThat(BlacklistCheckService.calculateRiskLevel(600, false)).isEqualTo(RiskLevel.LOW);
        assertThat(BlacklistCheckService.calculateRiskLevel(900, true)).isEqualTo(RiskLevel.HIGH);
    }
}
