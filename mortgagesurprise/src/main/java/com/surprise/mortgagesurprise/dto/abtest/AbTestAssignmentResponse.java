package com.surprise.mortgagesurprise.dto.abtest;

import java.util.UUID;

public record AbTestAssignmentResponse(
        UUID userId,
        String experimentName,
        String color
) {}
