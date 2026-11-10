package com.surprise.mortgagesurprise.dto.scenario;

import java.util.List;
import java.util.UUID;

public record ScenarioComparisonResponse(
        List<ScenarioResponse> scenarios,
        UUID lowestMonthlyPaymentScenarioId,
        UUID lowestOverpaymentScenarioId
) {}
