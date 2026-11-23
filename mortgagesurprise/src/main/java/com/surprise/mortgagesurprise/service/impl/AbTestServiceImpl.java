package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.ErrorCode;
import com.surprise.mortgagesurprise.dto.abtest.AbTestStatsResponse;
import com.surprise.mortgagesurprise.dto.abtest.UserTestStatDetail;
import com.surprise.mortgagesurprise.dto.abtest.VariantStat;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.entity.AbTestVariant;
import com.surprise.mortgagesurprise.model.entity.UserAbTestVariant;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import com.surprise.mortgagesurprise.repository.jpa.AbTestVariantsRepository;
import com.surprise.mortgagesurprise.repository.jpa.UserAbTestVariantRepository;
import com.surprise.mortgagesurprise.repository.mongo.UserActionAuditMongoRepository;
import com.surprise.mortgagesurprise.service.AbTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.surprise.mortgagesurprise.util.AbTestVariantUtil.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AbTestServiceImpl implements AbTestService {

    private final UserAbTestVariantRepository userVariantRepository;
    private final AbTestVariantsRepository variantsRepository;
    private final UserActionAuditMongoRepository auditRepository;

    @Override
    @Transactional
    public UserAbTestVariant getOrCreateVariant(UUID userId, String experimentName) {
        var normalizedExp = experimentName.trim();
        return userVariantRepository.findByUserIdAndExperimentName(userId, normalizedExp)
                .orElseGet(() -> assignVariant(userId, normalizedExp));
    }

    @Override
    @Transactional
    public UserAbTestVariant getOrCreateVariant(UUID userId, String experimentName, String preferredColor) {
        return getOrCreateVariant(userId, experimentName);
    }

    /**
     * New users are assigned using the latest experiment version stored in the DB:
     * hash -> bucket 0..99 -> cumulative percentages. The assignment is persisted,
     * so changing percentages later never moves users that already have a variant.
     */
    private UserAbTestVariant assignVariant(UUID userId, String experimentName) {
        var version = variantsRepository.findLatestVersion(experimentName)
                .orElseThrow(() -> new ResourceNotFoundException("A/B experiment not found: " + experimentName));
        var variants = variantsRepository.findAllByExperimentNameAndVersion(experimentName, version);

        var chosen = pickVariant(userId.toString(), experimentName, variants)
                .orElseThrow(() -> new BusinessException(ErrorCode.AB_TEST_INVALID_DISTRIBUTION,
                        "Variant probabilities of experiment '" + experimentName
                                + "' (version " + version + ") do not cover 100%"));

        return userVariantRepository.save(new UserAbTestVariant(userId, chosen));
    }

    @Override
    @Transactional(readOnly = true)
    public AbTestStatsResponse getStats(String experimentName) {
        var normalizedExp = isExperimentNameValid(experimentName)
                ? experimentName.trim()
                : DEFAULT_EXPERIMENT_NAME;

        var assignments = userVariantRepository.findByExperimentName(normalizedExp);

        var audits = auditRepository.findByActionTypeOrderByTimestampDesc(resolveActionType(normalizedExp));
        if (audits == null) {
            audits = Collections.emptyList();
        }
        var clickCountByUserId = getClickCountsByUserId(audits);

        List<UserTestStatDetail> userDetails = assignments.stream()
                .map(a -> new UserTestStatDetail(
                        a.getUserId(),
                        a.getAbTestVariant().getVariantName(),
                        clickCountByUserId.getOrDefault(a.getUserId(), 0L),
                        a.getCreatedAt() != null ? a.getCreatedAt() : Instant.now()))
                .toList();

        long totalClicks = userDetails.stream().mapToLong(UserTestStatDetail::clicksCount).sum();

        return new AbTestStatsResponse(normalizedExp, assignments.size(), totalClicks,
                buildVariantStats(definedVariantNames(normalizedExp), userDetails), userDetails);
    }

    private List<String> definedVariantNames(String experimentName) {
        return variantsRepository.findLatestVersion(experimentName)
                .map(version -> variantsRepository.findAllByExperimentNameAndVersion(experimentName, version))
                .orElse(Set.of())
                .stream()
                .map(AbTestVariant::getVariantName)
                .sorted()
                .toList();
    }

    private UserAuditActionType resolveActionType(String experimentName) {
        var lower = experimentName.toLowerCase();
        if (lower.contains("microloan") || lower.contains("apply")) {
            return UserAuditActionType.MICROLOAN_APPLICATION;
        }
        return UserAuditActionType.MORTGAGE_CALCULATION;
    }

    private Map<UUID, Long> getClickCountsByUserId(List<UserActionAuditDocument> audits) {
        return audits.stream()
                .filter(a -> a.getUserId() != null)
                .collect(Collectors.groupingBy(UserActionAuditDocument::getUserId, Collectors.counting()));
    }

    /** Every variant defined in the DB is shown (even with 0 users), plus any legacy names seen in assignments. */
    private List<VariantStat> buildVariantStats(List<String> definedNames, List<UserTestStatDetail> userDetails) {
        Map<String, List<UserTestStatDetail>> byVariant = new LinkedHashMap<>();
        definedNames.forEach(name -> byVariant.put(name, new ArrayList<>()));
        userDetails.forEach(d -> byVariant.computeIfAbsent(d.color(), k -> new ArrayList<>()).add(d));

        return byVariant.entrySet().stream()
                .map(e -> {
                    long users = e.getValue().size();
                    long clicks = e.getValue().stream().mapToLong(UserTestStatDetail::clicksCount).sum();
                    return new VariantStat(e.getKey(), users, clicks, users > 0 ? (double) clicks / users : 0.0);
                })
                .toList();
    }
}
