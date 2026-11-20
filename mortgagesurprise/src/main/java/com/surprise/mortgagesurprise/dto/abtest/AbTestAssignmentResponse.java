package com.surprise.mortgagesurprise.dto.abtest;

import com.surprise.mortgagesurprise.model.entity.UserAbTestVariant;

import java.util.UUID;

public record AbTestAssignmentResponse(
        UUID userId,
        String experimentName,
        String color
) {
    public static AbTestAssignmentResponse from(UserAbTestVariant value) {
        return new AbTestAssignmentResponse(value.getUserId(), value.getExperimentName(), value.getColor());
    }
}
