package com.surprise.mortgagesurprise.dto.audit;

public record MicroloanApplicationResponse(
        boolean success,
        String applicationId,
        String status,
        String message
) {}
