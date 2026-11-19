package com.surprise.mortgagesurprise.adapter;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanRateDTO;

import java.util.List;

public interface MicroloanRateAdapterService {

    MicroloanRateDTO findByName(String name);

    List<MicroloanRateDTO> findAll();

    MicroloanRateDTO save(MicroloanRateDTO dto);

    MicroloanRateDTO update(MicroloanRateDTO dto, Integer id);

    void delete(Integer id);
}
