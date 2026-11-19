package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanRateDTO;
import com.surprise.mortgagesurprise.model.entity.MicroloanRate;

import java.util.List;

public interface MicroloanRateService {
    MicroloanRate findById(Integer id);

    MicroloanRate findByName(String name);

    List<MicroloanRate> findAll();

    MicroloanRate save(MicroloanRateDTO dto);

    MicroloanRate update(MicroloanRateDTO dto, Integer id);

    void delete(Integer id);
}
