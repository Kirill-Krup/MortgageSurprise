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

        var assignedColor = calculateDeterministicVariant(userId.toString(), normalizedExp);

        return variantRepository.save(new UserAbTestVariant(userId, normalizedExp, assignedColor));
    }

    @Override
    @Transactional
    public UserAbTestVariant getOrCreateVariant(UUID userId, String experimentName, String preferredColor) {
        return getOrCreateVariant(userId, experimentName);
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

        Map<UUID, Long> clickCountByUserId = audits.stream()
                .filter(a -> a.getUserId() != null)
                .collect(Collectors.groupingBy(UserActionAuditDocument::getUserId, Collectors.counting()));

        List<UserTestStatDetail> userDetails = new ArrayList<>();
        long totalClicks = 0;

        for (var assignment : assignments) {
            var id = assignment.getUserId();
            long clicks = clickCountByUserId.getOrDefault(id, 0L);
            totalClicks += clicks;
            userDetails.add(new UserTestStatDetail(
                    id,
                    assignment.getColor(),
                    clicks,
                    assignment.getCreatedAt() != null ? assignment.getCreatedAt() : Instant.now()
            ));
        }

        Map<String, List<UserTestStatDetail>> byColor = userDetails.stream()
                .collect(Collectors.groupingBy(UserTestStatDetail::color));

        List<VariantStat> variantStats = new ArrayList<>();
        for (var color : DEFAULT_COLORS) {
            var usersWithColor = byColor.getOrDefault(color, List.of());
            long usersCount = usersWithColor.size();
            long colorClicks = usersWithColor.stream()
                    .mapToLong(UserTestStatDetail::clicksCount)
                    .sum();
            double avgClicks = usersCount > 0 ? (double) colorClicks / usersCount : 0.0;
            variantStats.add(new VariantStat(color, usersCount, colorClicks, avgClicks));
        }
        for (var entry : byColor.entrySet()) {
            if (!DEFAULT_COLORS.contains(entry.getKey())) {
                long count = entry.getValue().size();
                long clicks = entry.getValue()
                        .stream()
                        .mapToLong(UserTestStatDetail::clicksCount)
                        .sum();
                double avg = count > 0 ? (double) clicks / count : 0.0;
                variantStats.add(new VariantStat(entry.getKey(), count, clicks, avg));
            }
        }
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
}