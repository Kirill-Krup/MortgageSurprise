package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.admin.AdminLoanApplicationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/loan-applications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminLoanApplicationController {

    /**
     * Возвращает список заявок на микрозаймы для скоринга и одобрения администратором.
     * Заглушка, возвращающая пустой список, пока коллега завершает скоринг-сервис.
     */
    @GetMapping
    public ResponseEntity<List<AdminLoanApplicationDto>> getLoanApplications() {
        log.debug("GET /api/v1/admin/loan-applications - stub returning empty list");
        return ResponseEntity.ok(Collections.emptyList());
    }

    /**
     * Одобрение заявки на микрозайм администратором (при скоринге > 0.5).
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveApplication(@PathVariable String id) {
        log.info("Admin approved loan application {}", id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "applicationId", id,
                "status", "APPROVED",
                "message", "Заявка на микрозайм успешно одобрена"
        ));
    }

    /**
     * Отклонение заявки на микрозайм администратором.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectApplication(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = (body != null && body.containsKey("reason"))
                ? body.get("reason")
                : "Отклонено администратором";
        log.info("Admin rejected loan application {}: {}", id, reason);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "applicationId", id,
                "status", "REJECTED",
                "message", "Заявка отклонена",
                "reason", reason
        ));
    }
}
