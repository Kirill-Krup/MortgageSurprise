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

import java.math.BigDecimal;
import java.util.List;

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
    @Transactional(readOnly = true)
    public List<MicroloanRate> findAll() {
        var list = microloanRateRepository.findAll();
        if (list.isEmpty()) {
            // Seed default if database is empty
            var seeded = microloanRateRepository.save(new MicroloanRate("Стандарт", new BigDecimal("0.8000"), 7, 30));
            return List.of(seeded);
        }
        return list;
    }

    @Override
    @Transactional
    public MicroloanRate save(MicroloanRateDTO dto) {
        int minDay = dto.minDayRequired() != null ? dto.minDayRequired() : 7;
        int maxDay = dto.maxDayRequired() != null ? dto.maxDayRequired() : 30;
        var entity = microloanRateRepository.save(new MicroloanRate(dto.name(), dto.baseDailyRate(),
                minDay, maxDay));
        var cache = caffeineCacheManager.getCache(CacheConfig.MICROLOAN_RATES);
        if (cache != null && entity.getName() != null) {
            cache.evict(entity.getName());
        }
        return entity;
    }

    @Override
    @Transactional
    public MicroloanRate update(MicroloanRateDTO dto, Integer id) {
        var existingMicroloanRate = microloanRateRepository.findById(id)
                .orElseThrow(() -> new MicroLoanRateNotFoundException("Microloan with this id not found"));
        var oldName = existingMicroloanRate.getName();
        existingMicroloanRate.setName(dto.name());
        existingMicroloanRate.setBaseDailyRate(dto.baseDailyRate());
        if (dto.minDayRequired() != null) {
            existingMicroloanRate.setMinDayRequired(dto.minDayRequired());
        }
        if (dto.maxDayRequired() != null) {
            existingMicroloanRate.setMaxDayRequired(dto.maxDayRequired());
        }
        var result = microloanRateRepository.save(existingMicroloanRate);
        entityManager.detach(result);
        var cache = caffeineCacheManager.getCache(CacheConfig.MICROLOAN_RATES);
        if (cache != null) {
            cache.evict(oldName);
            cache.evict(result.getName());
        }
        return result;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        var existing = microloanRateRepository.findById(id)
                .orElseThrow(() -> new MicroLoanRateNotFoundException("Microloan with id " + id + " not found"));
        var name = existing.getName();
        microloanRateRepository.delete(existing);
        var cache = caffeineCacheManager.getCache(CacheConfig.MICROLOAN_RATES);
        if (cache != null && name != null) {
            cache.evict(name);
        }
    }
}
