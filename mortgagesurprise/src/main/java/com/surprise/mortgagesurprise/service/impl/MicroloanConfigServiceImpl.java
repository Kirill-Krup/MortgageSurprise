package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanConfigDTO;
import com.surprise.mortgagesurprise.model.entity.MicroloanConfig;
import com.surprise.mortgagesurprise.repository.jpa.MicroloanConfigRepository;
import com.surprise.mortgagesurprise.service.MicroloanConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MicroloanConfigServiceImpl implements MicroloanConfigService {

    private static final String DEFAULT_ID = "DEFAULT";
    private final MicroloanConfigRepository repository;

    @Override
    @Transactional(readOnly = true)
    public MicroloanConfig getConfig() {
        return repository.findById(DEFAULT_ID)
                .orElseGet(() -> new MicroloanConfig(
                        DEFAULT_ID,
                        new BigDecimal("0.8000"),
                        new BigDecimal("5.0000"),
                        Instant.now()
                ));
    }

    @Override
    @Transactional
    public MicroloanConfig updateConfig(MicroloanConfigDTO dto) {
        MicroloanConfig config = repository.findById(DEFAULT_ID)
                .orElseGet(() -> new MicroloanConfig(DEFAULT_ID, dto.baseDailyRate(), dto.vanitySurchargeRate(), Instant.now()));

        config.setBaseDailyRate(dto.baseDailyRate());
        config.setVanitySurchargeRate(dto.vanitySurchargeRate());
        config.setUpdatedAt(Instant.now());

        return repository.save(config);
    }
}
