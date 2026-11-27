package com.bank.simulator.controller;

import com.surprise.common.dto.simulator.RegisterEventRequest;
import com.surprise.common.dto.simulator.TrustEventResponse;
import com.bank.simulator.service.TrustEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class TrustEventController {

    private final TrustEventService trustEventService;

    @PostMapping
    public ResponseEntity<TrustEventResponse> registerEvent(
            @Valid @RequestBody RegisterEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trustEventService.registerEvent(request));
    }
}
