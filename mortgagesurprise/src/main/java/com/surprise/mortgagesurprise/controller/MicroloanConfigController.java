package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.adapter.MicroloanConfigAdapter;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanConfigDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MicroloanConfigController {

    private final MicroloanConfigAdapter configService;

    @GetMapping("/microloan/config")
    public ResponseEntity<MicroloanConfigDTO> getConfig() {
        return ResponseEntity.ok(configService.getConfig());
    }

    @PutMapping("/admin/microloan/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MicroloanConfigDTO> updateConfig(@Valid @RequestBody MicroloanConfigDTO dto) {
        return ResponseEntity.ok(configService.updateConfig(dto));
    }
}