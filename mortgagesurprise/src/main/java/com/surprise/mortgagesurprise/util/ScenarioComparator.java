package com.surprise.mortgagesurprise.util;

import com.surprise.mortgagesurprise.dto.ErrorCode;
import com.surprise.mortgagesurprise.dto.scenario.ScenarioComparisonResponse;
import com.surprise.mortgagesurprise.dto.scenario.ScenarioResponse;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.model.entity.CalculationScenario;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;


@Component
public class ScenarioComparator {

    private final int scenarioMinSize = 2;
    private final int scenarioMaxSize = 3;

    public ScenarioComparisonResponse compare(List<CalculationScenario> scenarios) {
        if (isValidScenario(scenarios)) {
            throw new BusinessException(ErrorCode.SCENARIO_COMPARE_COUNT);
        }
        var responses = scenarios.stream()
                .map(ScenarioResponse::from)
                .toList();

        var lowestPaymentId = scenarios.stream()
                .min(Comparator.comparing(CalculationScenario::getMonthlyPayment))
                .map(CalculationScenario::getId)
                .orElse(null);

        var lowestInterestId = scenarios.stream()
                .min(Comparator.comparing(CalculationScenario::getTotalInterest))
                .map(CalculationScenario::getId)
                .orElse(null);

        return new ScenarioComparisonResponse(responses, lowestPaymentId, lowestInterestId);
    }

    private boolean isValidScenario(List<CalculationScenario> scenarios) {
        return scenarios == null || scenarios.size() < scenarioMinSize || scenarios.size() > scenarioMaxSize;
    }
}