package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.adapter.CalculatorAdapter;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationRequest;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationResponse;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResponse;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.CalculatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/calculator")
@RequiredArgsConstructor
public class CalculatorController {

    private final CalculatorService calculatorService;
    private final CalculatorAdapter calculatorAdapter;

    @PostMapping({"/calculate", "/mortgage"})
    public ResponseEntity<CalculationResponse> calculate(@Valid @RequestBody CalculationRequest request,
                                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(calculatorAdapter.calculate(request, userDetails));
    }

    @PostMapping("/microloan")
    public ResponseEntity<MicroloanCalculationResponse> calculateMicroloan(@Valid @RequestBody MicroloanCalculationRequest request,
                                                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(calculatorAdapter.calculateMicroloan(request, userDetails));
    }

    @PostMapping("/microloan/apply")
    public ResponseEntity<MicroloanApplicationResponse> applyMicroloan(@Valid @RequestBody MicroloanApplicationRequest request,
                                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(calculatorAdapter.applyMicroloan(request, userDetails));
    }

    @GetMapping("/schedules/{scheduleId}")
    public ResponseEntity<PaymentScheduleDocument> getSchedule(@PathVariable String scheduleId) {
        return ResponseEntity.ok(calculatorService.getScheduleById(scheduleId));
    }
}