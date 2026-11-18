package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanConfigDTO;
import com.surprise.mortgagesurprise.model.entity.MicroloanConfig;
import com.surprise.mortgagesurprise.model.enums.Currency;
import com.surprise.mortgagesurprise.repository.jpa.MicroloanConfigRepository;
import com.surprise.mortgagesurprise.service.MicroloanConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
                        new BigDecimal("2000")
                ));
    }

    @Override
    @Transactional
    public MicroloanConfig updateConfig(MicroloanConfigDTO dto) {
        var config = repository.findById(DEFAULT_ID)
                .orElseGet(() -> new MicroloanConfig(DEFAULT_ID, dto.bankInterest()));

        config.setBankInterest(dto.bankInterest());

        return repository.save(config);
    }
}
