package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.scenario.SaveScenarioRequest;
import com.surprise.mortgagesurprise.dto.scenario.ScenarioComparisonResponse;
import com.surprise.mortgagesurprise.dto.scenario.ScenarioResponse;

import java.util.List;
import java.util.UUID;

public interface ScenarioService {
    ScenarioResponse saveScenario(UUID userId, SaveScenarioRequest req);
    List<ScenarioResponse> getUserScenarios(UUID userId);
    ScenarioResponse getScenarioById(UUID userId, UUID scenarioId);
    ScenarioComparisonResponse compareScenarios(UUID userId, List<UUID> scenarioIds);
    void deleteScenario(UUID userId, UUID scenarioId);
    void requestPdfExport(UUID userId, String userEmail, UUID scenarioId);
}
