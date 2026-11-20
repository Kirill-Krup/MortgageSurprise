package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.profile.PassportDataDTO;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.model.entity.UserPassportData;
import com.surprise.mortgagesurprise.repository.jpa.UserPassportDataRepository;
import com.surprise.mortgagesurprise.util.AesEncryptionService;
import com.surprise.mortgagesurprise.util.Sha256Encoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPassportServiceImplTest {

    @Mock
    private UserPassportDataRepository repository;

    @Spy
    private AesEncryptionService encryptionService = new AesEncryptionService("v8y/B?E(H+MbQeThWmZq4t7w!z%C*F-J");

    @InjectMocks
    private UserPassportServiceImpl passportService;

    private UUID userId;
    private PassportDataDTO sampleDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleDto = new PassportDataDTO(
                "Иванов Иван Иванович",
                "4515 123456",
                "2020-05-15",
                "770-001",
                "ОВД Тверского района г. Москвы"
        );
    }

    @Test
    @DisplayName("Should encrypt and decrypt roundtrip correctly in AesEncryptionService")
    void testAesRoundtrip() {
        String original = "4515 123456";
        String encrypted = encryptionService.encrypt(original);
        assertThat(encrypted).isNotEqualTo(original);

        String decrypted = encryptionService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    @DisplayName("Should return decrypted passport data for user")
    void shouldGetPassportData() {
        UserPassportData entity = new UserPassportData();
        entity.setUserId(userId);
        entity.setFullName(sampleDto.fullName());
        entity.setSeriesNumberEncrypted(encryptionService.encrypt(sampleDto.seriesNumber()));
        entity.setPassportHash(Sha256Encoder.toSha256("4515123456"));
        entity.setIssueDate(sampleDto.issueDate());
        entity.setDepartmentCode(sampleDto.departmentCode());
        entity.setIssuedBy(sampleDto.issuedBy());

        when(repository.findByUserId(userId)).thenReturn(Optional.of(entity));

        Optional<PassportDataDTO> result = passportService.getPassportData(userId);

        assertThat(result).isPresent();
        assertThat(result.get().fullName()).isEqualTo(sampleDto.fullName());
        assertThat(result.get().seriesNumber()).isEqualTo(sampleDto.seriesNumber());
        assertThat(result.get().departmentCode()).isEqualTo(sampleDto.departmentCode());
    }

    @Test
    @DisplayName("Should save passport with AES encryption and SHA-256 hash")
    void shouldSavePassportData() {
        when(repository.existsByPassportHashAndUserIdNot(any(), eq(userId))).thenReturn(false);
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());
        when(repository.save(any(UserPassportData.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PassportDataDTO saved = passportService.saveOrUpdatePassportData(userId, sampleDto);

        assertThat(saved).isNotNull();
        assertThat(saved.seriesNumber()).isEqualTo("4515 123456");

        verify(repository).save(argThat(entity -> {
            assertThat(entity.getFullName()).isEqualTo(sampleDto.fullName());
            assertThat(entity.getSeriesNumberEncrypted()).isNotEqualTo(sampleDto.seriesNumber());
            assertThat(entity.getPassportHash()).isEqualTo(Sha256Encoder.toSha256("4515123456"));
            return true;
        }));
    }

    @Test
    @DisplayName("Should throw BusinessException if passport hash belongs to another user")
    void shouldRejectDuplicatePassportHash() {
        when(repository.existsByPassportHashAndUserIdNot(any(), eq(userId))).thenReturn(true);

        assertThatThrownBy(() -> passportService.saveOrUpdatePassportData(userId, sampleDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("уже привязан к другому профилю");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete passport data by userId")
    void shouldDeletePassportData() {
        passportService.deletePassportData(userId);
        verify(repository).deleteByUserId(userId);
    }
}
