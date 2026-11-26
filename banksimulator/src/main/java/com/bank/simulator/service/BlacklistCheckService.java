package com.bank.simulator.service;

import com.bank.simulator.config.TrustProperties;
import com.surprise.common.dto.simulator.BlacklistCheckRequest;
import com.surprise.common.dto.simulator.BlacklistCheckResponse;
import com.bank.simulator.model.Subject;
import com.bank.simulator.repository.SubjectRepository;
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
public class BlacklistCheckService {

    private final SubjectRepository subjectRepository;
    private final TrustProperties trustProperties;
    private final DocumentHashService documentHashService;

    @Transactional(readOnly = true)
    public BlacklistCheckResponse check(BlacklistCheckRequest request) {
        String hash = resolveDocumentHash(request);
        log.debug("Checking blacklist status for hash: {}", hash);

        Optional<Subject> subjectOpt = subjectRepository.findByDocumentHash(hash);

        if (subjectOpt.isEmpty()) {
            return BlacklistCheckResponse.builder()
                    .blacklisted(false)
                    .trustScore(trustProperties.defaultInitialScore())
                    .riskLevel(RiskLevel.LOW)
                    .hardBlock(false)
                    .reasons(Collections.emptyList())
                    .build();
        }

        Subject subject = subjectOpt.get();
        int score = subject.getTrustScore();
        boolean hardBlock = subject.isHardBlock();
        boolean scoreBelowThreshold = score < trustProperties.blacklistThreshold();
        boolean isBlacklisted = hardBlock || scoreBelowThreshold;

        List<String> reasons = new ArrayList<>();
        if (hardBlock) {
            reasons.add("HARD_BLOCK: " + (subject.getBlockReason() != null ? subject.getBlockReason() : "Block applied"));
        }
        if (scoreBelowThreshold) {
            reasons.add("SCORE_BELOW_THRESHOLD: " + score + " < " + trustProperties.blacklistThreshold());
        }

        RiskLevel riskLevel = calculateRiskLevel(score, hardBlock);

        return BlacklistCheckResponse.builder()
                .blacklisted(isBlacklisted)
                .trustScore(score)
                .riskLevel(riskLevel)
                .hardBlock(hardBlock)
                .reasons(reasons)
                .build();
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

    public static RiskLevel calculateRiskLevel(int trustScore, boolean hardBlock) {
        if (hardBlock || trustScore < 300) {
            return RiskLevel.HIGH;
        } else if (trustScore < 600) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }
}
