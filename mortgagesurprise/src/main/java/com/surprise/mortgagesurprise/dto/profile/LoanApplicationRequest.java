package com.surprise.mortgagesurprise.dto.profile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoanApplicationRequest(
        @NotNull(message = "Возраст обязателен для заполнения")
        @Min(value = 18, message = "Возраст должен быть не менее 18 лет")
        @Max(value = 120, message = "Возраст не может превышать 120 лет")
        Integer age,

        @NotBlank(message = "Страна обязательна для заполнения")
        @Size(max = 100, message = "Название страны не должно превышать 100 символов")
        String country,

        @NotBlank(message = "Город обязателен для заполнения")
        @Size(max = 100, message = "Название города не должно превышать 100 символов")
        String city,

        @NotBlank(message = "Пол обязателен для заполнения")
        @Size(max = 32, message = "Значение пола не должно превышать 32 символа")
        String gender
) {}
