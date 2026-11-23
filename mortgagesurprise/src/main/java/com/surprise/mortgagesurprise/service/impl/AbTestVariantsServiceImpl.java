package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.ErrorCode;
import com.surprise.mortgagesurprise.dto.abtest.*;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.model.entity.AbTestVariant;
import com.surprise.mortgagesurprise.repository.jpa.AbTestVariantsRepository;
import com.surprise.mortgagesurprise.service.AbTestVariantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AbTestVariantsServiceImpl implements AbTestVariantsService {

    private static final int TOTAL_PERCENT = 100;

    private static final String DEFAULT_MORTGAGE_EXP = "mortgage_calculate_button";
    private static final String DEFAULT_MICROLOAN_EXP = "apply_microloan_button";

    private final AbTestVariantsRepository abTestVariantsRepository;
    private final Map<String, ExperimentMeta> experimentMetadata = new ConcurrentHashMap<>();

    private record ExperimentMeta(
            String name,
            String description,
            String status,
            Boolean isCustom,
            String createdAt,
            String updatedAt
    ) {}

    @Override
    @Transactional(readOnly = true)
    public Set<AbTestVariant> findAllByExperimentNameAndVersion(String experimentName, Integer version) {
        return abTestVariantsRepository.findAllByExperimentNameAndVersion(experimentName, version);
    }

    @Override
    @Transactional
    public AbTestVariant save(CreateAbTestVariantRequest request) {
        var experimentName = request.experimentName().trim();
        var variantName = request.variantName().trim();

        var existing = abTestVariantsRepository.findAllByExperimentNameAndVersion(
                experimentName, request.version());

        if (existing.stream().anyMatch(v -> v.getVariantName().equals(variantName))) {
            throw new BusinessException(ErrorCode.AB_TEST_VARIANT_EXISTS,
                    "Variant '" + variantName + "' already exists in experiment '" + experimentName
                            + "' version " + request.version());
        }

        applyUpdates(existing, request.newVariants());

        var newVariant = new AbTestVariant(experimentName, variantName,
                request.variantProbability(), request.version());

        var all = new HashSet<>(existing);
        all.add(newVariant);
        requireTotalIs100(all, true);

        return abTestVariantsRepository.save(newVariant);
    }

    @Override
    @Transactional
    public Set<AbTestVariant> updateAll(UpdateAbTestVariantRequest request) {
        var existing = abTestVariantsRepository.findAllByExperimentNameAndVersion(
                request.experimentName().trim(), request.version());

        if (existing.isEmpty()) {
            throw new ResourceNotFoundException("No variants for experiment '" + request.experimentName()
                    + "' version " + request.version());
        }

        applyUpdates(existing, request.newVariants());
        requireTotalIs100(existing, false);

        return existing;
    }

    /** Applies percentages from the request to already existing variants. Only checks that the variant exists. */
    private void applyUpdates(Set<AbTestVariant> variants, List<UpdateVariantsPercent> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        var byId = variants.stream()
                .collect(Collectors.toMap(AbTestVariant::getId, Function.identity()));
        for (var update : updates) {
            var variant = byId.get(update.variantId());
            if (variant == null) {
                throw new ResourceNotFoundException("Вариант с id=" + update.variantId()
                        + " не найден в этом эксперименте и версии");
            }
            variant.setVariantProbability(update.percent());
        }
    }

    private void requireTotalIs100(Collection<AbTestVariant> variants, boolean hintAboutNewVariants) {
        int sum = variants.stream().mapToInt(AbTestVariant::getVariantProbability).sum();
        if (sum != TOTAL_PERCENT) {
            throw new BusinessException(ErrorCode.AB_TEST_INVALID_DISTRIBUTION,
                    "Сумма вероятностей всех вариантов должна быть 100, сейчас: " + sum
                            + (hintAboutNewVariants
                            ? ". Передайте newVariants, чтобы перераспределить проценты между вариантами."
                            : ""));
        }
    }

    @Override
    @Transactional
    public List<ExperimentConfigDTO> getAllExperiments() {
        Set<String> expNames = new LinkedHashSet<>();
        expNames.add(DEFAULT_MORTGAGE_EXP);
        expNames.add(DEFAULT_MICROLOAN_EXP);
        expNames.addAll(abTestVariantsRepository.findDistinctExperimentNames());

        List<ExperimentConfigDTO> result = new ArrayList<>();
        for (String expName : expNames) {
            result.add(getOrCreateExperimentConfig(expName));
        }
        return result;
    }

    @Override
    @Transactional
    public ExperimentConfigDTO getExperiment(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Experiment ID must not be blank");
        }
        return getOrCreateExperimentConfig(id.trim());
    }

    @Override
    @Transactional
    public ExperimentConfigDTO saveExperiment(ExperimentConfigDTO dto) {
        if (dto.id() == null || dto.id().isBlank()) {
            throw new IllegalArgumentException("Experiment ID must not be blank");
        }
        if (dto.variants() == null || dto.variants().isEmpty()) {
            throw new IllegalArgumentException("Experiment variants must not be empty");
        }

        int sum = dto.variants().stream()
                .mapToInt(v -> v.weight() != null ? v.weight() : 0)
                .sum();
        if (sum != TOTAL_PERCENT) {
            throw new BusinessException(ErrorCode.AB_TEST_INVALID_DISTRIBUTION,
                    "Сумма вероятностей всех вариантов должна быть 100, сейчас: " + sum);
        }

        String expId = dto.id().trim();
        int nextVersion = abTestVariantsRepository.findLatestVersion(expId)
                .map(v -> v + 1)
                .orElse(0);

        List<AbTestVariant> newVariants = dto.variants().stream()
                .map(v -> new AbTestVariant(expId, v.name().trim(), v.weight(), nextVersion))
                .toList();

        abTestVariantsRepository.saveAll(newVariants);

        String now = Instant.now().toString();
        ExperimentMeta existingMeta = experimentMetadata.get(expId);
        String createdAt = existingMeta != null && existingMeta.createdAt() != null
                ? existingMeta.createdAt()
                : (dto.createdAt() != null ? dto.createdAt() : now);

        ExperimentMeta meta = new ExperimentMeta(
                dto.name() != null ? dto.name() : expId,
                dto.description() != null ? dto.description() : "",
                dto.status() != null ? dto.status() : "ACTIVE",
                dto.isCustom() != null ? dto.isCustom() : (!isDefaultExperiment(expId)),
                createdAt,
                now
        );
        experimentMetadata.put(expId, meta);

        return new ExperimentConfigDTO(
                expId,
                meta.name(),
                meta.description(),
                dto.variants(),
                meta.status(),
                meta.isCustom(),
                meta.createdAt(),
                meta.updatedAt()
        );
    }

    @Override
    @Transactional
    public void deleteExperiment(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        String expId = id.trim();
        abTestVariantsRepository.deleteAllByExperimentName(expId);
        experimentMetadata.remove(expId);
    }

    private ExperimentConfigDTO getOrCreateExperimentConfig(String expId) {
        var optVersion = abTestVariantsRepository.findLatestVersion(expId);
        Integer version = optVersion.orElse(null);

        Set<AbTestVariant> variants;
        if (version == null) {
            // If default experiment not seeded yet, seed version 0
            if (isDefaultExperiment(expId)) {
                variants = seedDefaultExperiment(expId);
            } else {
                throw new ResourceNotFoundException("Experiment not found: " + expId);
            }
        } else {
            variants = abTestVariantsRepository.findAllByExperimentNameAndVersion(expId, version);
        }

        List<ExperimentVariantConfigDTO> variantDTOs = variants.stream()
                .sorted(Comparator.comparing(AbTestVariant::getId))
                .map(v -> new ExperimentVariantConfigDTO(v.getVariantName(), v.getVariantProbability(), null))
                .toList();

        ExperimentMeta meta = experimentMetadata.computeIfAbsent(expId, id -> {
            String now = Instant.now().toString();
            if (DEFAULT_MORTGAGE_EXP.equals(id)) {
                return new ExperimentMeta("Кнопка расчета ипотеки",
                        "Тестирование цвета основной кнопки расчета в ипотечном калькуляторе",
                        "ACTIVE", false, now, now);
            } else if (DEFAULT_MICROLOAN_EXP.equals(id)) {
                return new ExperimentMeta("Кнопка оформления микрозайма",
                        "Тестирование цвета кнопки подачи заявки на микрозайм",
                        "ACTIVE", false, now, now);
            } else {
                return new ExperimentMeta(id, "", "ACTIVE", true, now, now);
            }
        });

        return new ExperimentConfigDTO(
                expId,
                meta.name(),
                meta.description(),
                variantDTOs,
                meta.status(),
                meta.isCustom(),
                meta.createdAt(),
                meta.updatedAt()
        );
    }

    private Set<AbTestVariant> seedDefaultExperiment(String expId) {
        var blue = new AbTestVariant(expId, "blue", 50, 0);
        var emerald = new AbTestVariant(expId, "emerald", 50, 0);
        abTestVariantsRepository.save(blue);
        abTestVariantsRepository.save(emerald);
        return Set.of(blue, emerald);
    }

    private boolean isDefaultExperiment(String expId) {
        return DEFAULT_MORTGAGE_EXP.equals(expId) || DEFAULT_MICROLOAN_EXP.equals(expId);
    }
}
