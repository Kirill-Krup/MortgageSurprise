package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.profile.PassportDataDTO;

import java.util.Optional;
import java.util.UUID;

public interface UserPassportService {
    Optional<PassportDataDTO> getPassportData(UUID userId);
    PassportDataDTO saveOrUpdatePassportData(UUID userId, PassportDataDTO dto);
    void deletePassportData(UUID userId);
}
