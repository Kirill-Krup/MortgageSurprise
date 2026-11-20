package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.abtest.AbTestAssignmentResponse;
import com.surprise.mortgagesurprise.dto.abtest.AbTestStatsResponse;
import com.surprise.mortgagesurprise.dto.abtest.UserTestStatDetail;
import com.surprise.mortgagesurprise.dto.abtest.VariantStat;
import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.entity.User;
import com.surprise.mortgagesurprise.model.entity.UserAbTestVariant;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import com.surprise.mortgagesurprise.repository.jpa.UserAbTestVariantRepository;
import com.surprise.mortgagesurprise.repository.jpa.UserRepository;
import com.surprise.mortgagesurprise.repository.mongo.UserActionAuditMongoRepository;
import com.surprise.mortgagesurprise.service.AbTestService;
import com.surprise.mortgagesurprise.util.Sha256Encoder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AbTestServiceImpl implements AbTestService {

    private static final Logger log = LoggerFactory.getLogger(AbTestServiceImpl.class);
    private static final String SALT = "mortgage_surprise_ab_salt_2026";
    private static final List<String> DEFAULT_COLORS = List.of("blue", "emerald");

    private final UserAbTestVariantRepository variantRepository;
    private final UserActionAuditMongoRepository auditRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AbTestAssignmentResponse getOrCreateVariant(UUID userId, String experimentName) {
        if (experimentName == null || experimentName.isBlank()) {
            throw new IllegalArgumentException("Experiment name must not be blank");
        }

        String normalizedExp = experimentName.trim();

        if (userId == null) {
            String color = calculateDeterministicVariant("anonymous", normalizedExp);
            return new AbTestAssignmentResponse(null, normalizedExp, color);
        }

        Optional<UserAbTestVariant> existing = variantRepository.findByUserIdAndExperimentName(userId, normalizedExp);
        if (existing.isPresent()) {
            UserAbTestVariant variant = existing.get();
            return new AbTestAssignmentResponse(variant.getUserId(), variant.getExperimentName(), variant.getColor());
        }

        String assignedColor = calculateDeterministicVariant(userId.toString(), normalizedExp);

        UserAbTestVariant newVariant = new UserAbTestVariant(userId, normalizedExp, assignedColor);
        UserAbTestVariant saved = variantRepository.save(newVariant);
        log.info("Assigned deterministic A/B test variant '{}' for user {} in experiment '{}'", assignedColor, userId, normalizedExp);

        return new AbTestAssignmentResponse(saved.getUserId(), saved.getExperimentName(), saved.getColor());
    }

    @Override
    @Transactional
    public AbTestAssignmentResponse getOrCreateVariant(UUID userId, String experimentName, String preferredColor) {
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
        String normalizedExp = (experimentName != null && !experimentName.isBlank())
                ? experimentName.trim()
                : "mortgage_calculate_button";

        List<UserAbTestVariant> assignments = variantRepository.findByExperimentName(normalizedExp);

        UserAuditActionType actionType = resolveActionType(normalizedExp);

        List<UserActionAuditDocument> audits = auditRepository.findByActionTypeOrderByTimestampDesc(actionType);
        if (audits == null) {
            audits = List.of();
        }

        Map<UUID, Long> clickCountByUserId = audits.stream()
                .filter(a -> a.getUserId() != null)
                .collect(Collectors.groupingBy(UserActionAuditDocument::getUserId, Collectors.counting()));

        Map<UUID, String> emailByUserId = audits.stream()
                .filter(a -> a.getUserId() != null && a.getUserEmail() != null)
                .collect(Collectors.toMap(
                        UserActionAuditDocument::getUserId,
                        UserActionAuditDocument::getUserEmail,
                        (existing, replacement) -> existing
                ));

        List<UserTestStatDetail> userDetails = new ArrayList<>();
        long totalClicks = 0;

        for (UserAbTestVariant assignment : assignments) {
            UUID uId = assignment.getUserId();
            long clicks = clickCountByUserId.getOrDefault(uId, 0L);
            totalClicks += clicks;

            String email = emailByUserId.computeIfAbsent(uId, id ->
                    userRepository.findById(id).map(User::getEmail).orElse("user-" + id.toString().substring(0, 8))
            );

            userDetails.add(new UserTestStatDetail(
                    uId,
                    email,
                    assignment.getColor(),
                    clicks,
                    assignment.getCreatedAt() != null ? assignment.getCreatedAt() : Instant.now()
            ));
        }

        Map<String, List<UserTestStatDetail>> byColor = userDetails.stream()
                .collect(Collectors.groupingBy(UserTestStatDetail::color));

        List<VariantStat> variantStats = new ArrayList<>();
        for (String color : DEFAULT_COLORS) {
            List<UserTestStatDetail> usersWithColor = byColor.getOrDefault(color, List.of());
            long usersCount = usersWithColor.size();
            long colorClicks = usersWithColor.stream().mapToLong(UserTestStatDetail::clicksCount).sum();
            double avgClicks = usersCount > 0 ? (double) colorClicks / usersCount : 0.0;
            variantStats.add(new VariantStat(color, usersCount, colorClicks, avgClicks));
        }

        for (Map.Entry<String, List<UserTestStatDetail>> entry : byColor.entrySet()) {
            if (!DEFAULT_COLORS.contains(entry.getKey())) {
                long count = entry.getValue().size();
                long clicks = entry.getValue().stream().mapToLong(UserTestStatDetail::clicksCount).sum();
                double avg = count > 0 ? (double) clicks / count : 0.0;
                variantStats.add(new VariantStat(entry.getKey(), count, clicks, avg));
            }
        }

        return new AbTestStatsResponse(
                normalizedExp,
                assignments.size(),
                totalClicks,
                variantStats,
                userDetails
        );
    }

    private UserAuditActionType resolveActionType(String experimentName) {
        String lower = experimentName.toLowerCase();
        if (lower.contains("microloan") || lower.contains("apply")) {
            return UserAuditActionType.MICROLOAN_APPLICATION;
        }
        return UserAuditActionType.MORTGAGE_CALCULATION;
    }
}
