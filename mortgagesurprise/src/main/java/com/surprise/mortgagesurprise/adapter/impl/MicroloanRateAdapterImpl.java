package com.surprise.mortgagesurprise.adapter.impl;

import com.surprise.mortgagesurprise.adapter.MicroloanRateAdapter;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanRateDTO;
import com.surprise.mortgagesurprise.service.MicroloanRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MicroloanRateAdapterImpl implements MicroloanRateAdapter {

    private final MicroloanRateService microloanRateService;

    @Override
    public MicroloanRateDTO findById(Integer id) {
        return MicroloanRateDTO.from(microloanRateService.findById(id));
    }

    @Override
    public MicroloanRateDTO findByName(String name) {
        return MicroloanRateDTO.from(microloanRateService.findByName(name));
    }

    @Override
    public List<MicroloanRateDTO> findAll() {
        return microloanRateService.findAll().stream()
                .map(MicroloanRateDTO::from)
                .toList();
    }

    @Override
    public MicroloanRateDTO save(MicroloanRateDTO dto) {
        return MicroloanRateDTO.from(microloanRateService.save(dto));
    }

    @Override
    public MicroloanRateDTO update(MicroloanRateDTO dto, Integer id) {
        return MicroloanRateDTO.from(microloanRateService.update(dto, id));
    }

    @Override
    public void delete(Integer id) {
        microloanRateService.delete(id);
    }
}
