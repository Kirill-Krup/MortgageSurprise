package com.surprise.mortgagesurprise.adapter;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanConfigDTO;

public interface MicroloanConfigAdapterService {
    MicroloanConfigDTO getConfig();

    MicroloanConfigDTO updateConfig(MicroloanConfigDTO dto);
}
