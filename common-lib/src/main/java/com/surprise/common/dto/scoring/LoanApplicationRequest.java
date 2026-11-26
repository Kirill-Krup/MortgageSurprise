package com.surprise.common.dto.scoring;

public record LoanApplicationRequest(
        Integer age,
        String country,
        String city,
        String gender
) {}
