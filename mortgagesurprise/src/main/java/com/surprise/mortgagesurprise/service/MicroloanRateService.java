package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanRateDTO;
import com.surprise.mortgagesurprise.model.entity.MicroloanRate;

public interface MicroloanRateService {
    MicroloanRate findByName(String name);

    MicroloanRate save(MicroloanRateDTO dto);

    MicroloanRate update(MicroloanRateDTO dto, Integer id);
}