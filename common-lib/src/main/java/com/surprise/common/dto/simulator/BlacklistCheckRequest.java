package com.surprise.common.dto.simulator;

import lombok.Builder;

@Builder
public record BlacklistCheckRequest(
        DocumentType documentType,

        String documentHash,

        String documentNumber
) {}
