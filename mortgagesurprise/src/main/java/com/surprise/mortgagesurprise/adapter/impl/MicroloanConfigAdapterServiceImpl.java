package com.surprise.mortgagesurprise.adapter.impl;

import com.surprise.mortgagesurprise.adapter.MicroloanConfigAdapterService;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanConfigDTO;
import com.surprise.mortgagesurprise.service.MicroloanConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MicroloanConfigAdapterServiceImpl implements MicroloanConfigAdapterService {

    private final MicroloanConfigService microloanConfigService;

    @Override
    public MicroloanConfigDTO getConfig() {
        return MicroloanConfigDTO.from(microloanConfigService.getConfig());
    }

    @Override
    public MicroloanConfigDTO updateConfig(MicroloanConfigDTO dto) {
        return MicroloanConfigDTO.from(microloanConfigService.updateConfig(dto));
    }
}