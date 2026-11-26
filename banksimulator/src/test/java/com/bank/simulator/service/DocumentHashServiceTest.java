package com.bank.simulator.service;

import com.bank.simulator.config.TrustProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DocumentHashService Unit Tests")
class DocumentHashServiceTest {

    private DocumentHashService documentHashService;

    @BeforeEach
    void setUp() {
        TrustProperties properties = new TrustProperties(
                300,
                700,
                "test-hmac-secret-key-at-least-32-chars!",
                Map.of()
        );
        documentHashService = new DocumentHashService(properties);
    }

    @Test
    @DisplayName("Produces deterministic 64-char lowercase hex HMAC-SHA256 hash")
    void hash_ValidDocumentNumber_ReturnsDeterministicHexHash() {
        String hash1 = documentHashService.hash("4510 123456");
        String hash2 = documentHashService.hash("4510 123456");

        assertThat(hash1)
                .hasSize(64)
                .matches("[0-9a-f]{64}")
                .isEqualTo(hash2);
    }

    @Test
    @DisplayName("Normalizes whitespace, hyphens, and case before hashing")
    void hash_DifferentFormatting_ProducesSameHash() {
        String hash1 = documentHashService.hash("ab-12 34-56");
        String hash2 = documentHashService.hash("  AB123456  ");

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Produces different hashes when HMAC secret differs")
    void hash_DifferentSecrets_ProducesDifferentHashes() {
        DocumentHashService otherService = new DocumentHashService(
                new TrustProperties(300, 700, "another-secret-key-for-testing-12345", Map.of())
        );

        String hash1 = documentHashService.hash("4510123456");
        String hash2 = otherService.hash("4510123456");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when document number is null or blank")
    void hash_NullOrBlank_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> documentHashService.hash(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be empty");

        assertThatThrownBy(() -> documentHashService.hash("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    @DisplayName("Normalizes raw string by stripping spaces and hyphens and uppercasing")
    void normalize_RawInput_StripsSpacesAndHyphensAndUppercases() {
        assertThat(documentHashService.normalize("  ab-12 cd-34  ")).isEqualTo("AB12CD34");
    }
}
