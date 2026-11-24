package com.surprise.mortgagesurprise.dto.profile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PassportDataDTO(
        @NotBlank(message = "Full name is required")
        @Size(max = 255)
        String fullName,

        @NotBlank(message = "Passport series and number are required")
        @Size(min = 6, max = 20)
        String seriesNumber,

        @Size(max = 32)
        String issueDate,

        @Size(max = 32)
        String departmentCode,

        @Size(max = 255)
        String issuedBy,

        @Valid
        LoanApplicationRequest loanApplicationRequest
) {
    public PassportDataDTO(String fullName, String seriesNumber, String issueDate, String departmentCode, String issuedBy) {
        this(fullName, seriesNumber, issueDate, departmentCode, issuedBy, null);
    }
}
