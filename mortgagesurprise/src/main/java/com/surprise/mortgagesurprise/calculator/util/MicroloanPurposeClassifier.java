package com.surprise.mortgagesurprise.calculator.util;

import com.surprise.mortgagesurprise.model.enums.MicroloanType;

import java.util.List;

/**
 * Utility responsible for automatically classifying loan purposes
 * into BASIC_NEEDS or VANITY categories.
 */
public final class MicroloanPurposeClassifier {

    private static final List<String> VANITY_KEYWORDS = List.of(
            "айфон", "iphone", "apple", "машин", "авто", "тюнинг", "tuning", "диск",
            "клуб", "вечеринк", "бар", "ресторан", "бренд", "часы", "gucci", "прада",
            "prada", "louis", "луи", "понты", "статус", "luxur", "премиум", "золот",
            "ювелир", "украшен", "vip", "вип", "отдых", "развлечен", "гаджет", "мото", "байк"
    );

    private MicroloanPurposeClassifier() {}

    public static MicroloanType classify(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return MicroloanType.BASIC_NEEDS;
        }
        String normalized = purpose.toLowerCase().trim();
        for (String keyword : VANITY_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return MicroloanType.VANITY;
            }
        }
        return MicroloanType.BASIC_NEEDS;
    }
}
