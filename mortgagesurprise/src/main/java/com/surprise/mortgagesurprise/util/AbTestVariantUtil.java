package com.surprise.mortgagesurprise.util;

import java.util.List;

public final class AbTestVariantUtil {

    public static final String DEFAULT_EXPERIMENT_NAME = "mortgage_calculate_button";
    public static final List<String> DEFAULT_COLORS = List.of("blue", "emerald");
    public static final String SALT = "mortgage_surprise_ab_salt_2026";

    private AbTestVariantUtil() {
    }

    public static String calculateDeterministicVariant(String identifier, String experimentName) {
        String input = identifier + ":" + experimentName + ":" + SALT;
        String hashHex = Sha256Encoder.toSha256(input);

        long hashVal = Long.parseLong(hashHex.substring(0, 8), 16);
        int index = (int) (hashVal % DEFAULT_COLORS.size());
        return DEFAULT_COLORS.get(Math.abs(index));
    }

    public static boolean isExperimentNameValid(String experimentName) {
        return experimentName != null && !experimentName.isBlank();
    }
}