package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.scenario.ScenarioComparisonResponse;
import com.surprise.mortgagesurprise.dto.scenario.ScenarioResponse;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.model.entity.CalculationScenario;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Single responsibility: Compare calculation scenarios and determine winning metrics.
 */
@Component
public class ScenarioComparator {

    public ScenarioComparisonResponse compare(List<CalculationScenario> scenarios) {
        if (scenarios == null || scenarios.size() < 2 || scenarios.size() > 3) {
            throw new BusinessException("You must select between 2 and 3 scenarios to compare.");
        }

        List<ScenarioResponse> responses = scenarios.stream()
                .map(ScenarioResponse::from)
                .toList();

        UUID lowestPaymentId = scenarios.stream()
                .min(Comparator.comparing(CalculationScenario::getMonthlyPayment))
                .map(CalculationScenario::getId)
                .orElse(null);

        UUID lowestInterestId = scenarios.stream()
                .min(Comparator.comparing(CalculationScenario::getTotalInterest))
                .map(CalculationScenario::getId)
                .orElse(null);

        return new ScenarioComparisonResponse(responses, lowestPaymentId, lowestInterestId);
    }
}
