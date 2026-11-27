package com.bank.simulator.service;

import com.bank.simulator.dto.AddDocumentRequest;
import com.bank.simulator.dto.HardBlockRequest;
import com.surprise.common.dto.simulator.CreateSubjectRequest;
import com.surprise.common.dto.simulator.SubjectResponse;

import java.util.List;
import java.util.UUID;

public interface SubjectService {
    List<SubjectResponse> getAll(int page, int size);
    SubjectResponse getById(UUID id);
    SubjectResponse getByDocumentHash(String documentHash);
    SubjectResponse createSubject(CreateSubjectRequest request);
    SubjectResponse addDocument(UUID subjectId, AddDocumentRequest request);
    SubjectResponse hardBlock(UUID subjectId, HardBlockRequest request);
    SubjectResponse unblock(UUID subjectId);
    void delete(UUID subjectId);
}
