package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.config.CacheConfig;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanConfigDTO;
import com.surprise.mortgagesurprise.model.entity.MicroloanConfig;
import com.surprise.mortgagesurprise.repository.jpa.MicroloanConfigRepository;
import com.surprise.mortgagesurprise.service.MicroloanConfigService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MicroloanConfigServiceImpl implements MicroloanConfigService {

    private final MicroloanConfigRepository repository;
    private final EntityManager entityManager;

    private static final String DEFAULT_ID = "DEFAULT";
    private static final String CACHE_KEY = "'DEFAULT'";

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheManager = "caffeineCacheManager", cacheNames = CacheConfig.MICROLOAN_CONFIGS,
            key = CACHE_KEY)
    public MicroloanConfig getConfig() {
        var result = repository.findById(DEFAULT_ID).orElseGet(() -> new MicroloanConfig(DEFAULT_ID, new BigDecimal("2000")));
        entityManager.detach(result);
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(cacheManager = "caffeineCacheManager", cacheNames = CacheConfig.MICROLOAN_CONFIGS,
            key = CACHE_KEY)
    public MicroloanConfig updateConfig(MicroloanConfigDTO dto) {
        var config = repository.findById(DEFAULT_ID).orElseGet(() -> new MicroloanConfig(DEFAULT_ID, dto.bankInterest()));
        config.setBankInterest(dto.bankInterest());
        return repository.save(config);
    }
}