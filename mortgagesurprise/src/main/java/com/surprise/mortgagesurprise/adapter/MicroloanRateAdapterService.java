package com.surprise.mortgagesurprise.adapter;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanRateDTO;

public interface MicroloanRateAdapterService {

    MicroloanRateDTO findByName(String name);

    MicroloanRateDTO save(MicroloanRateDTO dto);

    MicroloanRateDTO update(MicroloanRateDTO dto, Integer id);
}
