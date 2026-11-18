package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanRateDTO;
import com.surprise.mortgagesurprise.exception.MicroLoanRateNotFoundException;
import com.surprise.mortgagesurprise.model.entity.MicroloanRate;
import com.surprise.mortgagesurprise.repository.jpa.MicroloanRateRepository;
import com.surprise.mortgagesurprise.service.MicroloanRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MicroloanRateServiceImpl implements MicroloanRateService {

    private final MicroloanRateRepository microloanRateRepository;

    @Override
    @Transactional(readOnly = true)
    public MicroloanRate findByName(String name) {
        return microloanRateRepository.findByName(name)
                .orElseThrow(() -> new MicroLoanRateNotFoundException("Microloan with this name not found"));
    }

    @Override
    @Transactional
    public MicroloanRate save(MicroloanRateDTO dto) {
        return microloanRateRepository.save(new MicroloanRate(dto.name(), dto.baseDailyRate(),
                dto.minDayRequired(), dto.maxDayRequired()));
    }

    @Override
    @Transactional
    public MicroloanRate update(MicroloanRateDTO dto, Integer id) {
        var existingMicroloanRate = microloanRateRepository.findById(id)
                .orElseThrow(() -> new MicroLoanRateNotFoundException("Microloan with this name not found"));
        existingMicroloanRate.setName(dto.name());
        existingMicroloanRate.setBaseDailyRate(dto.baseDailyRate());
        existingMicroloanRate.setMinDayRequired(dto.minDayRequired());
        existingMicroloanRate.setMaxDayRequired(dto.maxDayRequired());
        return microloanRateRepository.save(existingMicroloanRate);
    }
}