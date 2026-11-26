package com.bank.simulator.controller;

import com.bank.simulator.dto.*;
import com.surprise.common.dto.simulator.*;
import com.bank.simulator.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public ResponseEntity<List<SubjectResponse>> getAllSubjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(subjectService.getAll(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectResponse> getSubjectById(@PathVariable UUID id) {
        return ResponseEntity.ok(subjectService.getById(id));
    }

    @GetMapping("/by-document/{documentHash}")
    public ResponseEntity<SubjectResponse> getSubjectByDocumentHash(@PathVariable String documentHash) {
        return ResponseEntity.ok(subjectService.getByDocumentHash(documentHash));
    }

    @PostMapping
    public ResponseEntity<SubjectResponse> createSubject(@Valid @RequestBody CreateSubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.createSubject(request));
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<SubjectResponse> addDocument(
            @PathVariable UUID id,
            @Valid @RequestBody AddDocumentRequest request) {
        return ResponseEntity.ok(subjectService.addDocument(id, request));
    }

    @PostMapping("/{id}/hard-block")
    public ResponseEntity<SubjectResponse> hardBlockSubject(
            @PathVariable UUID id,
            @Valid @RequestBody HardBlockRequest request) {
        return ResponseEntity.ok(subjectService.hardBlock(id, request));
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity<SubjectResponse> unblockSubject(@PathVariable UUID id) {
        return ResponseEntity.ok(subjectService.unblock(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable UUID id) {
        subjectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
