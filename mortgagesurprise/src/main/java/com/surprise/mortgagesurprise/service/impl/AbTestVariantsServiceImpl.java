package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.abtest.CreateAbTestVariantRequest;
import com.surprise.mortgagesurprise.dto.abtest.UpdateAbTestVariantRequest;
import com.surprise.mortgagesurprise.dto.abtest.UpdateVariantsPercent;
import com.surprise.mortgagesurprise.model.entity.AbTestVariant;
import com.surprise.mortgagesurprise.repository.jpa.AbTestVariantsRepository;
import com.surprise.mortgagesurprise.service.AbTestVariantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AbTestVariantsServiceImpl implements AbTestVariantsService {

    private final AbTestVariantsRepository abTestVariantsRepository;

    @Override
    public Set<AbTestVariant> findAllByExperimentNameAndVersion(String experimentName, Integer version) {
        return abTestVariantsRepository.findAllByExperimentNameAndVersion(experimentName, version);
    }

    @Override
    @Transactional
    public AbTestVariant save(CreateAbTestVariantRequest request) {
        var existing = abTestVariantsRepository.findAllByExperimentNameAndVersion(
                request.experimentName(), request.version());

        applyUpdates(existing, request.newVariants());

        var newVariant = new AbTestVariant(
                request.experimentName(),
                request.variantName(),
                request.variantProbability());
        newVariant.setVersion(request.version());

        var all = new HashSet<>(existing);
        all.add(newVariant);

        int sum = sumProbabilities(all);
        if (sum != 100) {
            throw new IllegalArgumentException(String.format(
                    "Сумма вероятностей всех вариантов должна быть 100, сейчас: %d. " +
                            "Передайте newVariants, чтобы перераспределить проценты между вариантами.", sum));
        }
        return abTestVariantsRepository.save(newVariant);
    }


    @Override
    @Transactional
    public Set<AbTestVariant> updateAll(UpdateAbTestVariantRequest request) {
        var existing = abTestVariantsRepository.findAllByExperimentNameAndVersion(
                request.experimentName(), request.version());

        applyUpdates(existing, request.newVariants());

        if (!isOneHundred(existing)) {
            throw new IllegalArgumentException(
                    "Сумма вероятностей всех вариантов должна быть 100, сейчас: "
                            + sumProbabilities(existing));
        }

        return abTestVariantsRepository.saveAll(existing);
    }

    /**
     * Применяет проценты из запроса к уже существующим вариантам.
     * Никакой валидации не делает — только проверяет, что вариант существует.
     */
    private void applyUpdates(Set<AbTestVariant> variants, List<UpdateVariantsPercent> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        var byId = variants.stream()
                .collect(Collectors.toMap(AbTestVariant::getId, Function.identity()));
        for (var update : updates) {
            var variant = byId.get(update.variantId());
            if (variant == null) {
                throw new IllegalArgumentException(
                        "Вариант с id=" + update.variantId() + " не найден");
            }
            variant.setVariantProbability(update.percent());
        }
    }

    private boolean isOneHundred(Set<AbTestVariant> variants) {
        return sumProbabilities(variants) == 100;
    }

    private int sumProbabilities(Set<AbTestVariant> variants) {
        return variants.stream()
                .mapToInt(AbTestVariant::getVariantProbability)
                .sum();
    }
}