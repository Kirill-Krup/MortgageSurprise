package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.service.CalculatorService;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calculator")
public class CalculatorController {

    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<CalculationResponse> calculate(
            @Valid @RequestBody CalculationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails != null ? userDetails.getId() : null;
        CalculationResponse response = calculatorService.calculateAndPersist(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/schedules/{scheduleId}")
    public ResponseEntity<PaymentScheduleDocument> getSchedule(@PathVariable String scheduleId) {
        PaymentScheduleDocument schedule = calculatorService.getScheduleById(scheduleId);
        return ResponseEntity.ok(schedule);
    }
}
