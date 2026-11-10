package com.surprise.mortgagesurprise.dto.scenario;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CompareScenariosRequest(
        @NotNull(message = "Scenario IDs are required")
        @Size(min = 2, max = 3, message = "Comparison requires between 2 and 3 scenarios")
        List<UUID> scenarioIds
) {}
