package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanConfigDTO;
import com.surprise.mortgagesurprise.model.entity.MicroloanConfig;
import com.surprise.mortgagesurprise.service.MicroloanConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class MicroloanConfigController {

    private final MicroloanConfigService configService;

    public MicroloanConfigController(MicroloanConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/microloan/config")
    public ResponseEntity<MicroloanConfig> getConfig() {
        return ResponseEntity.ok(configService.getConfig());
    }

    @PutMapping("/admin/microloan/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MicroloanConfig> updateConfig(@Valid @RequestBody MicroloanConfigDTO dto) {
        return ResponseEntity.ok(configService.updateConfig(dto));
    }
}
