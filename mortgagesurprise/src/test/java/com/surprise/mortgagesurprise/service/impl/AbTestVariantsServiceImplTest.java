package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.ErrorCode;
import com.surprise.mortgagesurprise.dto.abtest.CreateAbTestVariantRequest;
import com.surprise.mortgagesurprise.dto.abtest.UpdateAbTestVariantRequest;
import com.surprise.mortgagesurprise.dto.abtest.UpdateVariantsPercent;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.model.entity.AbTestVariant;
import com.surprise.mortgagesurprise.repository.jpa.AbTestVariantsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbTestVariantsServiceImplTest {

    private static final String EXP = "exp";

    @Mock
    private AbTestVariantsRepository repository;

    @InjectMocks
    private AbTestVariantsServiceImpl service;

    private static AbTestVariant variant(int id, String name, int percent) {
        var v = new AbTestVariant(EXP, name, percent, 0);
        v.setId(id);
        return v;
    }

    private static void assertCode(Throwable t, ErrorCode expected) {
        assertThat(t).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getCode()).isEqualTo(expected));
    }

    @Test
    @DisplayName("updateAll rebalances percentages when the new sum is 100")
    void updateAllRebalances() {
        var a = variant(1, "a", 50);
        var b = variant(2, "b", 50);
        when(repository.findAllByExperimentNameAndVersion(EXP, 0)).thenReturn(Set.of(a, b));

        var result = service.updateAll(new UpdateAbTestVariantRequest(EXP, 0,
                List.of(new UpdateVariantsPercent(1, 70), new UpdateVariantsPercent(2, 30))));

        assertThat(result).hasSize(2);
        assertThat(a.getVariantProbability()).isEqualTo(70);
        assertThat(b.getVariantProbability()).isEqualTo(30);
    }

    @Test
    @DisplayName("updateAll rejects a distribution that does not sum to 100")
    void updateAllRejectsWrongSum() {
        when(repository.findAllByExperimentNameAndVersion(EXP, 0))
                .thenReturn(Set.of(variant(1, "a", 50), variant(2, "b", 50)));

        assertThatThrownBy(() -> service.updateAll(new UpdateAbTestVariantRequest(EXP, 0,
                List.of(new UpdateVariantsPercent(1, 80)))))
                .satisfies(t -> assertCode(t, ErrorCode.AB_TEST_INVALID_DISTRIBUTION));
    }

    @Test
    @DisplayName("updateAll fails for a variant id that is not part of the experiment version")
    void updateAllUnknownVariantId() {
        when(repository.findAllByExperimentNameAndVersion(EXP, 0)).thenReturn(Set.of(variant(1, "a", 100)));

        assertThatThrownBy(() -> service.updateAll(new UpdateAbTestVariantRequest(EXP, 0,
                List.of(new UpdateVariantsPercent(99, 100)))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateAll fails when the experiment version has no variants")
    void updateAllEmptyExperiment() {
        when(repository.findAllByExperimentNameAndVersion(EXP, 5)).thenReturn(Set.of());

        assertThatThrownBy(() -> service.updateAll(new UpdateAbTestVariantRequest(EXP, 5,
                List.of(new UpdateVariantsPercent(1, 100)))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("save adds a variant when percentages are redistributed to 100")
    void saveWithRedistribution() {
        var a = variant(1, "a", 100);
        when(repository.findAllByExperimentNameAndVersion(EXP, 0)).thenReturn(Set.of(a));
        when(repository.save(any(AbTestVariant.class))).thenAnswer(i -> i.getArgument(0));

        var saved = service.save(new CreateAbTestVariantRequest(EXP, 0, "b", 50,
                List.of(new UpdateVariantsPercent(1, 50))));

        assertThat(saved.getVariantName()).isEqualTo("b");
        assertThat(saved.getVariantProbability()).isEqualTo(50);
        assertThat(saved.getVersion()).isZero();
        assertThat(a.getVariantProbability()).isEqualTo(50);
    }

    @Test
    @DisplayName("save rejects a duplicate variant name within the same experiment version")
    void saveRejectsDuplicateName() {
        when(repository.findAllByExperimentNameAndVersion(EXP, 0)).thenReturn(Set.of(variant(1, "a", 100)));

        assertThatThrownBy(() -> service.save(new CreateAbTestVariantRequest(EXP, 0, "a", 0, null)))
                .satisfies(t -> assertCode(t, ErrorCode.AB_TEST_VARIANT_EXISTS));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("save rejects a distribution that does not sum to 100 and persists nothing")
    void saveRejectsWrongSum() {
        when(repository.findAllByExperimentNameAndVersion(EXP, 0)).thenReturn(Set.of(variant(1, "a", 100)));

        assertThatThrownBy(() -> service.save(new CreateAbTestVariantRequest(EXP, 0, "b", 50, null)))
                .satisfies(t -> assertCode(t, ErrorCode.AB_TEST_INVALID_DISTRIBUTION));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("saveExperiment creates new version and persists all variants when sum is 100")
    void saveExperimentSucceeds() {
        when(repository.findLatestVersion("mortgage_calculate_button")).thenReturn(java.util.Optional.of(0));

        var dto = new com.surprise.mortgagesurprise.dto.abtest.ExperimentConfigDTO(
                "mortgage_calculate_button",
                "Кнопка расчета ипотеки",
                "Описание",
                List.of(
                        new com.surprise.mortgagesurprise.dto.abtest.ExperimentVariantConfigDTO("blue", 0, null),
                        new com.surprise.mortgagesurprise.dto.abtest.ExperimentVariantConfigDTO("emerald", 0, null),
                        new com.surprise.mortgagesurprise.dto.abtest.ExperimentVariantConfigDTO("Red", 100, null)
                ),
                "ACTIVE",
                false,
                null,
                null
        );

        var saved = service.saveExperiment(dto);

        assertThat(saved.id()).isEqualTo("mortgage_calculate_button");
        assertThat(saved.variants()).hasSize(3);
        verify(repository).saveAll(any());
    }

    @Test
    @DisplayName("saveExperiment rejects weights not summing to 100")
    void saveExperimentRejectsInvalidWeights() {
        var dto = new com.surprise.mortgagesurprise.dto.abtest.ExperimentConfigDTO(
                "mortgage_calculate_button",
                "Кнопка расчета ипотеки",
                "Описание",
                List.of(
                        new com.surprise.mortgagesurprise.dto.abtest.ExperimentVariantConfigDTO("blue", 50, null),
                        new com.surprise.mortgagesurprise.dto.abtest.ExperimentVariantConfigDTO("Red", 60, null)
                ),
                "ACTIVE",
                false,
                null,
                null
        );

        assertThatThrownBy(() -> service.saveExperiment(dto))
                .satisfies(t -> assertCode(t, ErrorCode.AB_TEST_INVALID_DISTRIBUTION));
        verify(repository, never()).saveAll(any());
    }
}
