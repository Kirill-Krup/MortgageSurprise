package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.adapter.MicroloanRateAdapterService;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanRateDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MicroloanRateControllerTest {

    @Mock
    private MicroloanRateAdapterService adapterService;

    @InjectMocks
    private MicroloanRateController controller;

    @Test
    @DisplayName("getAllRates should return list of rates")
    void shouldReturnAllRates() {
        MicroloanRateDTO dto = new MicroloanRateDTO(1, "Стандарт", new BigDecimal("0.8000"), 7, 30);
        when(adapterService.findAll()).thenReturn(List.of(dto));

        ResponseEntity<List<MicroloanRateDTO>> response = controller.getAllRates();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(dto);
    }

    @Test
    @DisplayName("saveRate should call adapter service and return created status")
    void shouldSaveRate() {
        MicroloanRateDTO dto = new MicroloanRateDTO(null, "Стандарт", new BigDecimal("0.8000"), 7, 30);
        MicroloanRateDTO saved = new MicroloanRateDTO(1, "Стандарт", new BigDecimal("0.8000"), 7, 30);
        when(adapterService.save(dto)).thenReturn(saved);

        ResponseEntity<MicroloanRateDTO> response = controller.saveRate(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(saved);
    }

    @Test
    @DisplayName("deleteRate should call adapter service and return noContent")
    void shouldDeleteRate() {
        ResponseEntity<Void> response = controller.deleteRate(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(adapterService).delete(1);
    }
}
