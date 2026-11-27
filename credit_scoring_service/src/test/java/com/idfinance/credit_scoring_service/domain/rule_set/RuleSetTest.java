package com.idfinance.credit_scoring_service.domain.rule_set;

import com.idfinance.credit_scoring_service.model.RuleType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleSetTest {

    @Test
    void ageRuleSet_inclusiveAndExclusiveWorkCorrectly() {
        AgeRuleSet inclusive = new AgeRuleSet(1, 18, 65, RuleType.INCLUSIVE, 0.3f);
        assertTrue(inclusive.apply(18));
        assertTrue(inclusive.apply(30));
        assertTrue(inclusive.apply(65));
        assertFalse(inclusive.apply(17));
        assertFalse(inclusive.apply(66));
        assertFalse(inclusive.apply(null));

        AgeRuleSet exclusive = new AgeRuleSet(2, 18, 65, RuleType.EXCLUSIVE, -0.2f);
        assertTrue(exclusive.apply(17));
        assertTrue(exclusive.apply(66));
        assertFalse(exclusive.apply(18));
        assertFalse(exclusive.apply(30));
        assertFalse(exclusive.apply(65));
        assertFalse(exclusive.apply(null));
    }

    @Test
    void stringRuleSets_handleTrimCaseInsensitivityAndNulls() {
        GenderRuleSet gender = new GenderRuleSet(1, "Female", RuleType.INCLUSIVE, 0.1f);
        assertTrue(gender.apply("  female "));
        assertFalse(gender.apply("male"));
        assertFalse(gender.apply(null));

        CountryRuleSet country = new CountryRuleSet(1, "RU", RuleType.EXCLUSIVE, -0.5f);
        assertFalse(country.apply(" ru "));
        assertTrue(country.apply("US"));
        assertFalse(country.apply(null));

        CityRuleSet city = new CityRuleSet(1L, "Moscow", RuleType.INCLUSIVE, 0.2f);
        assertTrue(city.apply("moscow"));
        assertFalse(city.apply("Kazan"));
        assertFalse(city.apply(null));
    }
}
