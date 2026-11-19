package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanRateDTO;
import com.surprise.mortgagesurprise.exception.MicroLoanRateNotFoundException;
import com.surprise.mortgagesurprise.model.entity.MicroloanRate;
import com.surprise.mortgagesurprise.repository.jpa.MicroloanRateRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MicroloanRateServiceImplTest {

    @Mock
    private MicroloanRateRepository microloanRateRepository;

    @Mock
    private CaffeineCacheManager caffeineCacheManager;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private MicroloanRateServiceImpl microloanRateService;

    private MicroloanRate testRate;

    @BeforeEach
    void setUp() {
        testRate = new MicroloanRate("Стандарт", new BigDecimal("0.8000"), 7, 30);
        testRate.setId(1);
    }

    @Test
    @DisplayName("findByName should return rate if exists")
    void shouldFindByName() {
        when(microloanRateRepository.findByName("Стандарт")).thenReturn(Optional.of(testRate));

        MicroloanRate result = microloanRateService.findByName("Стандарт");

        assertThat(result.getName()).isEqualTo("Стандарт");
        assertThat(result.getBaseDailyRate()).isEqualByComparingTo("0.8000");
        verify(entityManager).detach(testRate);
    }

    @Test
    @DisplayName("findByName should throw exception when not found")
    void shouldThrowWhenNotFound() {
        when(microloanRateRepository.findByName("Несуществующий")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> microloanRateService.findByName("Несуществующий"))
                .isInstanceOf(MicroLoanRateNotFoundException.class);
    }

    @Test
    @DisplayName("findAll should return all rates from repository")
    void shouldFindAll() {
        when(microloanRateRepository.findAll()).thenReturn(List.of(testRate));

        List<MicroloanRate> list = microloanRateService.findAll();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("Стандарт");
    }

    @Test
    @DisplayName("save should persist new rate and evict cache")
    void shouldSaveRate() {
        MicroloanRateDTO dto = new MicroloanRateDTO("Лайт", new BigDecimal("0.6000"), 14, 30);
        when(microloanRateRepository.save(any(MicroloanRate.class))).thenReturn(testRate);
        when(caffeineCacheManager.getCache(anyString())).thenReturn(cache);

        MicroloanRate saved = microloanRateService.save(dto);

        assertThat(saved).isNotNull();
        verify(microloanRateRepository).save(any(MicroloanRate.class));
        verify(cache).evict(anyString());
    }

    @Test
    @DisplayName("delete should remove rate and evict cache")
    void shouldDeleteRate() {
        when(microloanRateRepository.findById(1)).thenReturn(Optional.of(testRate));
        when(caffeineCacheManager.getCache(anyString())).thenReturn(cache);

        microloanRateService.delete(1);

        verify(microloanRateRepository).delete(testRate);
        verify(cache).evict("Стандарт");
    }
}
