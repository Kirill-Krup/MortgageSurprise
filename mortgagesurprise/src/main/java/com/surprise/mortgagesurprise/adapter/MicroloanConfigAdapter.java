package com.surprise.mortgagesurprise.adapter;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanConfigDTO;

public interface MicroloanConfigAdapter {
    MicroloanConfigDTO getConfig();

    MicroloanConfigDTO updateConfig(MicroloanConfigDTO dto);
}
