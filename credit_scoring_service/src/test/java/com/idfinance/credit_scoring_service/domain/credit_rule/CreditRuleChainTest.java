package com.idfinance.credit_scoring_service.domain.credit_rule;

import com.idfinance.credit_scoring_service.domain.rule_set.AgeRuleSet;
import com.idfinance.credit_scoring_service.domain.rule_set.CityRuleSet;
import com.idfinance.credit_scoring_service.domain.rule_set.CountryRuleSet;
import com.idfinance.credit_scoring_service.domain.rule_set.GenderRuleSet;
import com.idfinance.credit_scoring_service.dto.CreditDecisionResponse;
import com.idfinance.credit_scoring_service.dto.LoanApplicationRequest;
import com.idfinance.credit_scoring_service.model.RuleType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreditRuleChainTest {

    @Test
    void executeChain_approvesWhenTotalScoreAtLeastHalf() {
        CreditRuleChain chain = new CreditChainFactory(
                () -> Set.of(new AgeRuleSet(1, 18, 60, RuleType.INCLUSIVE, 0.3f)),
                () -> Set.of(new GenderRuleSet(1, "FEMALE", RuleType.INCLUSIVE, 0.1f)),
                () -> Set.of(new CountryRuleSet(1, "RU", RuleType.INCLUSIVE, 0.2f)),
                () -> Set.of(new CityRuleSet(1L, "Moscow", RuleType.INCLUSIVE, 0.1f))
        ).create();

        CreditDecisionResponse response = chain.executeChain(
                new LoanApplicationRequest(30, "RU", "Moscow", "FEMALE"));

        assertTrue(response.approved());
        assertNull(response.reason());
        assertEquals(0.7f, response.score(), 0.0001f);
    }

    @Test
    void executeChain_rejectsWhenTotalScoreBelowHalf() {
        CreditRuleChain chain = new CreditChainFactory(
                () -> Set.of(new AgeRuleSet(1, 18, 60, RuleType.INCLUSIVE, 0.2f)),
                () -> Set.of(new GenderRuleSet(1, "FEMALE", RuleType.INCLUSIVE, 0.1f)),
                () -> Set.of(new CountryRuleSet(1, "RU", RuleType.INCLUSIVE, 0.1f)),
                () -> Set.of(new CityRuleSet(1L, "Moscow", RuleType.INCLUSIVE, 0.05f))
        ).create();

        CreditDecisionResponse response = chain.executeChain(
                new LoanApplicationRequest(30, "RU", "Moscow", "FEMALE"));

        assertFalse(response.approved());
        assertEquals("Your credit score is too low", response.reason());
        assertEquals(0f, response.score());
    }

    @Test
    void executeChain_doesNotAccumulateScoreAcrossRequestsAndHandlesNullFields() {
        CreditRuleChain chain = new CreditChainFactory(
                () -> Set.of(new AgeRuleSet(1, 18, 60, RuleType.INCLUSIVE, 0.3f)),
                Set::of,
                () -> Set.of(new CountryRuleSet(1, "RU", RuleType.INCLUSIVE, 0.1f)),
                Set::of
        ).create();

        CreditDecisionResponse first = chain.executeChain(
                new LoanApplicationRequest(30, "RU", null, null));
        assertFalse(first.approved());

        // Second request must start from 0, not accumulate 0.4 from the first request
        CreditDecisionResponse second = chain.executeChain(
                new LoanApplicationRequest(30, "RU", null, null));
        assertFalse(second.approved());

        CreditDecisionResponse empty = chain.executeChain(
                new LoanApplicationRequest(null, null, null, null));
        assertFalse(empty.approved());
    }
}
