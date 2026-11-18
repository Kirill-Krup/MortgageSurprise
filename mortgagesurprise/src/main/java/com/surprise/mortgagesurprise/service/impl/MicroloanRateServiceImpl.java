package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.config.CacheConfig;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanRateDTO;
import com.surprise.mortgagesurprise.exception.MicroLoanRateNotFoundException;
import com.surprise.mortgagesurprise.model.entity.MicroloanRate;
import com.surprise.mortgagesurprise.repository.jpa.MicroloanRateRepository;
import com.surprise.mortgagesurprise.service.MicroloanRateService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MicroloanRateServiceImpl implements MicroloanRateService {

    private final MicroloanRateRepository microloanRateRepository;
    private final CaffeineCacheManager caffeineCacheManager;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheManager = "caffeineCacheManager", cacheNames = CacheConfig.MICROLOAN_RATES,
            key = "#name")
    public MicroloanRate findByName(String name) {
        var result = microloanRateRepository.findByName(name)
                .orElseThrow(() -> new MicroLoanRateNotFoundException("Microloan with this name not found"));
        entityManager.detach(result);
        return result;
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
        var oldName = existingMicroloanRate.getName();
        existingMicroloanRate.setName(dto.name());
        existingMicroloanRate.setBaseDailyRate(dto.baseDailyRate());
        existingMicroloanRate.setMinDayRequired(dto.minDayRequired());
        existingMicroloanRate.setMaxDayRequired(dto.maxDayRequired());
        var result = microloanRateRepository.save(existingMicroloanRate);
        entityManager.detach(result);
        var cache = caffeineCacheManager.getCache(CacheConfig.MICROLOAN_RATES);
        if (cache != null) {
            cache.evict(oldName);
            cache.evict(result.getName());
        }
        return result;
    }
}