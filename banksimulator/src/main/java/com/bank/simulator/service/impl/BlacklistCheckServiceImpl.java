package com.bank.simulator.service.impl;

import com.bank.simulator.config.TrustProperties;
import com.bank.simulator.model.Subject;
import com.bank.simulator.repository.SubjectRepository;
import com.bank.simulator.service.BlacklistCheckService;
import com.bank.simulator.service.DocumentHashService;
import com.surprise.common.dto.simulator.BlacklistCheckRequest;
import com.surprise.common.dto.simulator.BlacklistCheckResponse;
import com.surprise.common.dto.simulator.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlacklistCheckServiceImpl implements BlacklistCheckService {

    private final SubjectRepository subjectRepository;
    private final TrustProperties trustProperties;
    private final DocumentHashService documentHashService;

    @Override
    @Transactional(readOnly = true)
    public BlacklistCheckResponse check(BlacklistCheckRequest request) {
        String hash = resolveDocumentHash(request);
        log.debug("Checking blacklist status for hash: {}", hash);

        Optional<Subject> subjectOpt = subjectRepository.findByDocumentHash(hash);
        if (subjectOpt.isEmpty()) {
            return buildCleanResponse();
        }
        Subject subject = subjectOpt.get();
        int score = subject.getTrustScore();
        List<String> reasons = buildReasons(subject);
        RiskLevel riskLevel = trustProperties.calculateRiskLevel(score,  subject.isHardBlock());

        return BlacklistCheckResponse.builder()
                .blacklisted(subject.isBlacklisted(trustProperties.blacklistThreshold()))
                .trustScore(score)
                .riskLevel(riskLevel)
                .hardBlock( subject.isHardBlock())
                .reasons(reasons)
                .build();
    }

    private BlacklistCheckResponse buildCleanResponse() {
        return BlacklistCheckResponse.builder()
                .blacklisted(false)
                .trustScore(trustProperties.defaultInitialScore())
                .riskLevel(RiskLevel.LOW)
                .hardBlock(false)
                .reasons(Collections.emptyList())
                .build();
    }

    private List<String> buildReasons(Subject subject) {
        List<String> reasons = new ArrayList<>();
        if (subject.isHardBlock()) {
            String reason = subject.getBlockReason() != null ? subject.getBlockReason() : "Block applied";
            reasons.add("HARD_BLOCK: " + reason);
        }
        int threshold = trustProperties.blacklistThreshold();
        if (subject.getTrustScore() < threshold) {
            reasons.add("SCORE_BELOW_THRESHOLD: " + subject.getTrustScore() + " < " + threshold);
        }
        return reasons;
    }

    private String resolveDocumentHash(BlacklistCheckRequest request) {
        if (request.documentHash() != null && !request.documentHash().isBlank()) {
            return request.documentHash().trim();
        }
        if (request.documentNumber() != null && !request.documentNumber().isBlank()) {
            return documentHashService.hash(request.documentNumber());
        }
        throw new IllegalArgumentException("Either documentHash or documentNumber must be provided");
    }
}
