package com.bank.simulator.service;

import com.bank.simulator.config.TrustProperties;
import com.bank.simulator.dto.AddDocumentRequest;
import com.surprise.common.dto.simulator.CreateSubjectRequest;
import com.bank.simulator.dto.HardBlockRequest;
import com.surprise.common.dto.simulator.SubjectResponse;
import com.bank.simulator.model.Subject;
import com.bank.simulator.model.SubjectDocument;
import com.bank.simulator.repository.SubjectDocumentRepository;
import com.bank.simulator.repository.SubjectRepository;
import com.surprise.common.dto.simulator.DocumentType;
import com.surprise.common.dto.simulator.RiskLevel;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubjectService Unit Tests")
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private SubjectDocumentRepository documentRepository;

    private SubjectService subjectService;

    @BeforeEach
    void setUp() {
        TrustProperties properties = new TrustProperties(300, 700, "secret", Map.of());
        subjectService = new SubjectService(subjectRepository, documentRepository, properties);
    }

    @Test
    @DisplayName("Returns paginated list of subjects sorted by createdAt descending")
    void getAll_ReturnsMappedSubjects() {
        Subject subject = Subject.builder()
                .id(UUID.randomUUID())
                .trustScore(700)
                .hardBlock(false)
                .build();
        when(subjectRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(subject)));

        List<SubjectResponse> result = subjectService.getAll(0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(subject.getId());
        assertThat(result.getFirst().trustScore()).isEqualTo(700);
    }

    @Test
    @DisplayName("Returns subject by ID when found")
    void getById_Found_ReturnsSubjectResponse() {
        UUID id = UUID.randomUUID();
        Subject subject = Subject.builder().id(id).trustScore(500).hardBlock(false).build();
        when(subjectRepository.findById(id)).thenReturn(Optional.of(subject));

        SubjectResponse response = subjectService.getById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(response.blacklisted()).isFalse();
    }

    @Test
    @DisplayName("Throws EntityNotFoundException when subject ID does not exist")
    void getById_NotFound_ThrowsEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(subjectRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.getById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    @DisplayName("Finds subject by trimmed document hash")
    void getByDocumentHash_Found_ReturnsSubjectResponse() {
        Subject subject = Subject.builder().id(UUID.randomUUID()).trustScore(750).build();
        when(subjectRepository.findByDocumentHash("hash-123")).thenReturn(Optional.of(subject));

        SubjectResponse response = subjectService.getByDocumentHash("  hash-123  ");

        assertThat(response.id()).isEqualTo(subject.getId());
    }

    @Test
    @DisplayName("Throws EntityNotFoundException when subject is not found by document hash")
    void getByDocumentHash_NotFound_ThrowsEntityNotFoundException() {
        when(subjectRepository.findByDocumentHash("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.getByDocumentHash("missing"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Creates subject with default initial score when initialScore is null")
    void createSubject_DefaultScore_SavesSubjectAndDocument() {
        when(documentRepository.existsByDocumentHash("doc-hash")).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> {
            Subject s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SubjectResponse response = subjectService.createSubject(
                CreateSubjectRequest.builder()
                        .documentType(DocumentType.PASSPORT)
                        .documentHash("  doc-hash  ")
                        .build()
        );

        assertThat(response.trustScore()).isEqualTo(700);
        assertThat(response.hardBlock()).isFalse();
        assertThat(response.documents()).hasSize(1);
        assertThat(response.documents().getFirst().documentHash()).isEqualTo("doc-hash");
    }

    @Test
    @DisplayName("Creates subject with custom initial score when provided")
    void createSubject_CustomScore_UsesProvidedScore() {
        when(documentRepository.existsByDocumentHash("doc-hash")).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse response = subjectService.createSubject(
                CreateSubjectRequest.builder()
                        .documentType(DocumentType.PERSONAL_ID)
                        .documentHash("doc-hash")
                        .initialScore(250)
                        .build()
        );

        assertThat(response.trustScore()).isEqualTo(250);
        assertThat(response.blacklisted()).isTrue();
        assertThat(response.riskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when creating subject with already registered document hash")
    void createSubject_DuplicateHash_ThrowsIllegalArgumentException() {
        when(documentRepository.existsByDocumentHash("dup-hash")).thenReturn(true);

        CreateSubjectRequest request = CreateSubjectRequest.builder()
                .documentType(DocumentType.PASSPORT)
                .documentHash("dup-hash")
                .build();

        assertThatThrownBy(() -> subjectService.createSubject(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
        verify(subjectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Links new document to existing subject")
    void addDocument_ValidRequest_LinksDocument() {
        UUID id = UUID.randomUUID();
        Subject subject = Subject.builder().id(id).trustScore(700).build();
        when(subjectRepository.findById(id)).thenReturn(Optional.of(subject));
        when(documentRepository.existsByDocumentHash("new-hash")).thenReturn(false);
        when(subjectRepository.save(subject)).thenReturn(subject);

        SubjectResponse response = subjectService.addDocument(
                id,
                AddDocumentRequest.builder()
                        .documentType(DocumentType.PERSONAL_ID)
                        .documentHash("  new-hash ")
                        .build()
        );

        assertThat(response.documents()).hasSize(1);
        assertThat(response.documents().getFirst().documentHash()).isEqualTo("new-hash");
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when adding document hash that is already linked")
    void addDocument_DuplicateHash_ThrowsIllegalArgumentException() {
        UUID id = UUID.randomUUID();
        Subject subject = Subject.builder().id(id).trustScore(700).build();
        when(subjectRepository.findById(id)).thenReturn(Optional.of(subject));
        when(documentRepository.existsByDocumentHash("existing-hash")).thenReturn(true);

        AddDocumentRequest request = AddDocumentRequest.builder()
                .documentType(DocumentType.PERSONAL_ID)
                .documentHash("existing-hash")
                .build();

        assertThatThrownBy(() -> subjectService.addDocument(id, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already linked");
    }

    @Test
    @DisplayName("Applies hard block and trims block reason")
    void hardBlock_ExistingSubject_SetsHardBlockAndReason() {
        UUID id = UUID.randomUUID();
        Subject subject = Subject.builder().id(id).trustScore(800).hardBlock(false).build();
        when(subjectRepository.findById(id)).thenReturn(Optional.of(subject));
        when(subjectRepository.save(subject)).thenReturn(subject);

        SubjectResponse response = subjectService.hardBlock(id, new HardBlockRequest("  Manual review  "));

        assertThat(response.hardBlock()).isTrue();
        assertThat(response.blacklisted()).isTrue();
        assertThat(response.blockReason()).isEqualTo("Manual review");
        assertThat(response.riskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    @DisplayName("Removes hard block and clears block reason")
    void unblock_ExistingSubject_ClearsHardBlockAndReason() {
        UUID id = UUID.randomUUID();
        Subject subject = Subject.builder()
                .id(id)
                .trustScore(800)
                .hardBlock(true)
                .blockReason("Old reason")
                .build();
        when(subjectRepository.findById(id)).thenReturn(Optional.of(subject));
        when(subjectRepository.save(subject)).thenReturn(subject);

        SubjectResponse response = subjectService.unblock(id);

        assertThat(response.hardBlock()).isFalse();
        assertThat(response.blacklisted()).isFalse();
        assertThat(response.blockReason()).isNull();
    }

    @Test
    @DisplayName("Deletes existing subject and throws EntityNotFoundException if missing")
    void delete_ExistingAndMissing_BehavesCorrectly() {
        UUID existingId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();

        when(subjectRepository.existsById(existingId)).thenReturn(true);
        when(subjectRepository.existsById(missingId)).thenReturn(false);

        subjectService.delete(existingId);
        verify(subjectRepository).deleteById(existingId);

        assertThatThrownBy(() -> subjectService.delete(missingId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
