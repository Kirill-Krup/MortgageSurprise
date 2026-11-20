package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.abtest.AbTestStatsResponse;
import com.surprise.mortgagesurprise.dto.abtest.UserTestStatDetail;
import com.surprise.mortgagesurprise.dto.abtest.VariantStat;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.entity.UserAbTestVariant;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
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

    private final UserAbTestVariantRepository variantRepository;
    private final UserActionAuditMongoRepository auditRepository;

    @Override
    @Transactional
    public UserAbTestVariant getOrCreateVariant(UUID userId, String experimentName) {
        var normalizedExp = experimentName.trim();
        var existing = variantRepository.findByUserIdAndExperimentName(userId, normalizedExp);
        if (existing.isPresent()) {
            return existing.get();
        }

        var variantName = calculateDeterministicVariant(userId.toString(), normalizedExp);

        return variantRepository.save(new UserAbTestVariant(userId, normalizedExp, variantName));
    }

    @Override
    @Transactional
    public UserAbTestVariant getOrCreateVariant(UUID userId, String experimentName, String preferredColor) {
        return getOrCreateVariant(userId, experimentName);
    }

    public static String calculateDeterministicVariant(String identifier, String experimentName) {
        String input = identifier + ":" + experimentName + ":" + SALT;
        long hash = Math.abs((long) input.hashCode());
        return DEFAULT_COLORS.get((int) (hash % DEFAULT_COLORS.size()));
    }

    @Override
    @Transactional(readOnly = true)
    public AbTestStatsResponse getStats(String experimentName) {
        var normalizedExp = isExperimentNameValid(experimentName)
                ? experimentName.trim()
                : DEFAULT_EXPERIMENT_NAME;

        var assignments = variantRepository.findByExperimentName(normalizedExp);

        var actionType = resolveActionType(normalizedExp);

        var audits = auditRepository.findByActionTypeOrderByTimestampDesc(actionType);
        if (audits == null) {
            audits = Collections.emptyList();
        }

        var clickCountByUserId = getClickCountsByUserId(audits);

        List<UserTestStatDetail> userDetails = new ArrayList<>();

        fillUserDetails(assignments, userDetails, clickCountByUserId);
        long totalClicks = getTotalClicks(assignments, clickCountByUserId);

        List<VariantStat> variantStats = new ArrayList<>();
        fillVariantStats(variantStats, userDetails);
        return new AbTestStatsResponse(normalizedExp, assignments.size(), totalClicks,
                variantStats, userDetails);
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

    private long getTotalClicks(List<UserAbTestVariant> assignments, Map<UUID, Long> clickCountByUserId) {
        long totalClicks = 0;
        for (var assignment : assignments) {
            var id = assignment.getUserId();
            long clicks = clickCountByUserId.getOrDefault(id, 0L);
            totalClicks += clicks;
        }
        return totalClicks;
    }

    private void fillUserDetails(List<UserAbTestVariant> assignments, List<UserTestStatDetail> userDetails,
                                 Map<UUID, Long> clickCountByUserId) {
        for (var assignment : assignments) {
            var id = assignment.getUserId();
            long clicks = clickCountByUserId.getOrDefault(id, 0L);
            userDetails.add(new UserTestStatDetail(
                    id,
                    assignment.getColor(),
                    clicks,
                    assignment.getCreatedAt() != null ? assignment.getCreatedAt() : Instant.now()
            ));
        }
    }

    private void fillVariantStats(List<VariantStat> variantStats, List<UserTestStatDetail> userDetails) {
        Map<String, List<UserTestStatDetail>> byColor = userDetails.stream()
                .collect(Collectors.groupingBy(UserTestStatDetail::color));
        fillInDefaultColors(variantStats, byColor);
        var colorsNotFound = byColor.entrySet()
                .stream()
                .filter(x -> !DEFAULT_COLORS.contains(x.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        fillInColorsNotFound(variantStats, colorsNotFound);
    }

    private void fillInDefaultColors(List<VariantStat> variantStats, Map<String, List<UserTestStatDetail>> byColor) {
        for (var color : DEFAULT_COLORS) {
            var usersWithColor = byColor.getOrDefault(color, List.of());
            long usersCount = usersWithColor.size();
            long colorClicks = usersWithColor.stream()
                    .mapToLong(UserTestStatDetail::clicksCount)
                    .sum();
            double avgClicks = usersCount > 0 ? (double) colorClicks / usersCount : 0.0;
            variantStats.add(new VariantStat(color, usersCount, colorClicks, avgClicks));
        }
    }

    private void fillInColorsNotFound(List<VariantStat> variantStats, Map<String, List<UserTestStatDetail>> byColor) {
        for (var entry : byColor.entrySet()) {
            long count = entry.getValue().size();
            long clicks = entry.getValue()
                    .stream()
                    .mapToLong(UserTestStatDetail::clicksCount)
                    .sum();
            double avg = count > 0 ? (double) clicks / count : 0.0;
            variantStats.add(new VariantStat(entry.getKey(), count, clicks, avg));
        }
    }
}