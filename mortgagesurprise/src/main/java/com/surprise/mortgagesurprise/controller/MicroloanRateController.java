package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.adapter.MicroloanRateAdapterService;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanRateDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MicroloanRateController {

    private final MicroloanRateAdapterService microloanRateAdapterService;

    @GetMapping("/microloan/rates")
    public ResponseEntity<List<MicroloanRateDTO>> getAllRates() {
        return ResponseEntity.ok(microloanRateAdapterService.findAll());
    }

    @GetMapping("/microloan/rate")
    public ResponseEntity<MicroloanRateDTO> getByName(@RequestParam String name) {
        return ResponseEntity.ok(microloanRateAdapterService.findByName(name));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/microloan/rate")
    public ResponseEntity<MicroloanRateDTO> saveRate(@Valid @RequestBody MicroloanRateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(microloanRateAdapterService.save(dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/microloan/rate/{id}")
    public ResponseEntity<MicroloanRateDTO> updateRate(@Valid @RequestBody MicroloanRateDTO dto,
                                                       @PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(microloanRateAdapterService.update(dto, id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/microloan/rate/{id}")
    public ResponseEntity<Void> deleteRate(@PathVariable Integer id) {
        microloanRateAdapterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
