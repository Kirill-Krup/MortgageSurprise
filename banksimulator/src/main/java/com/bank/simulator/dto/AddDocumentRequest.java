package com.bank.simulator.dto;

import com.surprise.common.dto.simulator.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AddDocumentRequest(
        @NotNull(message = "Document type is required")
        DocumentType documentType,

        @NotBlank(message = "Document hash is required")
        String documentHash
) {}
