package com.idfinance.credit_scoring_service.dto;

public record LoanApplication(Integer age, String country,
                              String city, String gender) {
}