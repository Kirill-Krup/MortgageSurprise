package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.scenario.CompareScenariosRequest;
import com.surprise.mortgagesurprise.dto.scenario.SaveScenarioRequest;
import com.surprise.mortgagesurprise.dto.scenario.ScenarioComparisonResponse;
import com.surprise.mortgagesurprise.dto.scenario.ScenarioResponse;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.ScenarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scenarios")
public class ScenarioController {

    private final ScenarioService scenarioService;

    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @PostMapping
    public ResponseEntity<ScenarioResponse> saveScenario(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SaveScenarioRequest request
    ) {
        ScenarioResponse response = scenarioService.saveScenario(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ScenarioResponse>> getUserScenarios(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(scenarioService.getUserScenarios(userDetails.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScenarioResponse> getScenarioById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(scenarioService.getScenarioById(userDetails.getId(), id));
    }

    @PostMapping("/compare")
    public ResponseEntity<ScenarioComparisonResponse> compareScenarios(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CompareScenariosRequest request
    ) {
        return ResponseEntity.ok(scenarioService.compareScenarios(userDetails.getId(), request.scenarioIds()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScenario(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        scenarioService.deleteScenario(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
