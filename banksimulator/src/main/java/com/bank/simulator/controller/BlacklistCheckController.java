package com.bank.simulator.controller;

import com.surprise.common.dto.simulator.BlacklistCheckRequest;
import com.surprise.common.dto.simulator.BlacklistCheckResponse;
import com.bank.simulator.service.BlacklistCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/blacklist")
@RequiredArgsConstructor
public class BlacklistCheckController {

    private final BlacklistCheckService blacklistCheckService;

    @PostMapping("/check")
    public ResponseEntity<BlacklistCheckResponse> checkSubject(
            @Valid @RequestBody BlacklistCheckRequest request) {
        return ResponseEntity.ok(blacklistCheckService.check(request));
    }
}
