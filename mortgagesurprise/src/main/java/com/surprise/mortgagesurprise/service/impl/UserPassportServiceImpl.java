package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.ErrorCode;
import com.surprise.mortgagesurprise.dto.profile.LoanApplicationRequest;
import com.surprise.mortgagesurprise.dto.profile.PassportDataDTO;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.model.entity.UserPassportData;
import com.surprise.mortgagesurprise.repository.jpa.UserPassportDataRepository;
import com.surprise.mortgagesurprise.service.UserPassportService;
import com.surprise.mortgagesurprise.util.AesEncryptionService;
import com.surprise.mortgagesurprise.util.HmacSha256Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPassportServiceImpl implements UserPassportService {

    private final UserPassportDataRepository repository;
    private final AesEncryptionService encryptionService;
    private final HmacSha256Service hmacSha256Service;

    @Override
    @Transactional(readOnly = true)
    public Optional<PassportDataDTO> getPassportData(UUID userId) {
        return repository.findByUserId(userId).map(entity -> {
            String decryptedSeriesNumber = encryptionService.decrypt(entity.getSeriesNumberEncrypted());
            LoanApplicationRequest loanApp = null;
            if (entity.getAge() != null || entity.getCountry() != null || entity.getCity() != null || entity.getGender() != null) {
                loanApp = new LoanApplicationRequest(
                        entity.getAge(),
                        entity.getCountry(),
                        entity.getCity(),
                        entity.getGender()
                );
            }
            return new PassportDataDTO(
                    entity.getFullName(),
                    decryptedSeriesNumber,
                    entity.getIssueDate(),
                    entity.getDepartmentCode(),
                    entity.getIssuedBy(),
                    loanApp
            );
        });
    }

    @Override
    @Transactional
    public PassportDataDTO saveOrUpdatePassportData(UUID userId, PassportDataDTO dto) {
        String normalized = dto.seriesNumber().replaceAll("[\\s-]+", "").toUpperCase().trim();
        String passportHash = hmacSha256Service.hash(normalized);

        if (repository.existsByPassportHashAndUserIdNot(passportHash, userId)) {
            throw new BusinessException(ErrorCode.PASSPORT_ALREADY_LINKED);
        }

        String encryptedSeriesNumber = encryptionService.encrypt(dto.seriesNumber().trim());

        UserPassportData entity = repository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPassportData newEntity = new UserPassportData();
                    newEntity.setUserId(userId);
                    return newEntity;
                });

        entity.setFullName(dto.fullName().trim());
        entity.setSeriesNumberEncrypted(encryptedSeriesNumber);
        entity.setPassportHash(passportHash);
        entity.setIssueDate(dto.issueDate());
        entity.setDepartmentCode(dto.departmentCode());
        entity.setIssuedBy(dto.issuedBy());

        if (dto.loanApplicationRequest() != null) {
            entity.setAge(dto.loanApplicationRequest().age());
            entity.setCountry(dto.loanApplicationRequest().country() != null ? dto.loanApplicationRequest().country().trim() : null);
            entity.setCity(dto.loanApplicationRequest().city() != null ? dto.loanApplicationRequest().city().trim() : null);
            entity.setGender(dto.loanApplicationRequest().gender() != null ? dto.loanApplicationRequest().gender().trim() : null);
        }

        repository.save(entity);
        log.info("Saved encrypted passport data for user {}", userId);

        return dto;
    }

    @Override
    @Transactional
    public void deletePassportData(UUID userId) {
        repository.deleteByUserId(userId);
        log.info("Deleted passport data for user {}", userId);
    }
}
