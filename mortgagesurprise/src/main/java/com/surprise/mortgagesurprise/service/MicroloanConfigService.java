package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanConfigDTO;
import com.surprise.mortgagesurprise.model.entity.MicroloanConfig;

public interface MicroloanConfigService {

    MicroloanConfig getConfig();

    MicroloanConfig updateConfig(MicroloanConfigDTO dto);
}