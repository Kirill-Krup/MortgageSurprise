package com.surprise.mortgagesurprise.util;

import com.surprise.mortgagesurprise.model.entity.AbTestVariant;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

public final class AbTestVariantUtil {

    public static final String DEFAULT_EXPERIMENT_NAME = "mortgage_calculate_button";
    public static final String SALT = "mortgage_surprise_ab_salt_2026";
    public static final int BUCKETS = 100;

    private AbTestVariantUtil() {
    }

    /**
     * Stable bucket in [0, 99] for (identifier, experiment). The first 8 hex chars of SHA-256 are
     * 32 bits and always fit into a non-negative long, so no Math.abs is needed.
     */
    public static int calculateBucket(String identifier, String experimentName) {
        String hashHex = Sha256Encoder.toSha256(identifier + ":" + experimentName + ":" + SALT);
        long hashVal = Long.parseLong(hashHex.substring(0, 8), 16);
        return (int) (hashVal % BUCKETS);
    }

    /**
     * Picks a variant by cumulative percentage. Variants are ordered by id so the result
     * does not depend on the (unordered) Set returned from the database.
     * Empty result means the percentages do not cover the bucket (misconfigured distribution).
     */
    public static Optional<AbTestVariant> pickVariant(String identifier, String experimentName,
                                                      Collection<AbTestVariant> variants) {
        int bucket = calculateBucket(identifier, experimentName);
        int upperBound = 0;
        var ordered = variants.stream()
                .sorted(Comparator.comparing(AbTestVariant::getId))
                .toList();
        for (var variant : ordered) {
            upperBound += variant.getVariantProbability();
            if (bucket < upperBound) {
                return Optional.of(variant);
            }
        }
        return Optional.empty();
    }

    public static boolean isExperimentNameValid(String experimentName) {
        return experimentName != null && !experimentName.isBlank();
    }
}
