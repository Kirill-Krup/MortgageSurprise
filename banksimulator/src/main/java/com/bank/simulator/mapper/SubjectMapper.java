package com.bank.simulator.mapper;

import com.bank.simulator.config.TrustProperties;
import com.bank.simulator.model.Subject;
import com.bank.simulator.model.SubjectDocument;
import com.surprise.common.dto.simulator.DocumentDto;
import com.surprise.common.dto.simulator.RiskLevel;
import com.surprise.common.dto.simulator.SubjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Component
public class SubjectMapper {

    private final TrustProperties trustProperties;

    public SubjectResponse toResponse(Subject subject) {
        int score = subject.getTrustScore();
        boolean hardBlock = subject.isHardBlock();
        boolean blacklisted = subject.isBlacklisted(trustProperties.blacklistThreshold());
        RiskLevel riskLevel = trustProperties.calculateRiskLevel(score, hardBlock);

        List<DocumentDto> docs = mapDocuments(subject.getDocuments());

        return SubjectResponse.builder()
                .id(subject.getId())
                .trustScore(score)
                .hardBlock(hardBlock)
                .blockReason(subject.getBlockReason())
                .riskLevel(riskLevel)
                .blacklisted(blacklisted)
                .documents(docs)
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .build();
    }

    private List<DocumentDto> mapDocuments(List<SubjectDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        return documents.stream()
                .map(doc -> DocumentDto.builder()
                        .id(doc.getId())
                        .documentType(doc.getDocumentType())
                        .documentHash(doc.getDocumentHash())
                        .createdAt(doc.getCreatedAt())
                        .build())
                .toList();
    }
}
