package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.ErrorCode;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.dto.scenario.SaveScenarioRequest;
import com.surprise.mortgagesurprise.dto.scenario.ScenarioComparisonResponse;
import com.surprise.mortgagesurprise.dto.scenario.ScenarioResponse;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.messaging.event.PdfExportEvent;
import com.surprise.mortgagesurprise.messaging.producer.MessageProducer;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.model.entity.CalculationScenario;
import com.surprise.mortgagesurprise.repository.jpa.CalculationScenarioRepository;
import com.surprise.mortgagesurprise.service.MortgageCalculator;
import com.surprise.mortgagesurprise.service.PaymentScheduleService;
import com.surprise.mortgagesurprise.util.ScenarioComparator;
import com.surprise.mortgagesurprise.service.ScenarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Single responsibility: Manage calculation scenario lifecycle (CRUD, persistence orchestration, comparison delegation, export dispatching).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ScenarioServiceImpl implements ScenarioService {

    private final CalculationScenarioRepository scenarioRepository;
    private final MortgageCalculator mortgageCalculator;
    private final PaymentScheduleService paymentScheduleService;
    private final ScenarioComparator scenarioComparator;
    private final MessageProducer messageProducer;

    @Override
    public ScenarioResponse saveScenario(UUID userId, SaveScenarioRequest req) {
        CalculationRequest calcRequest = new CalculationRequest(
                req.propertyPrice(),
                req.downPayment(),
                req.termMonths(),
                req.annualInterestRate(),
                req.paymentType(),
                req.currency(),
                null,
                req.earlyRepayments()
        );

        CalculationResult result = mortgageCalculator.calculate(calcRequest);
        PaymentScheduleDocument savedSchedule = paymentScheduleService.saveSchedule(result);

        CalculationScenario scenario = new CalculationScenario(
                userId,
                req.title(),
                result.propertyPrice(),
                result.downPayment(),
                result.loanAmount(),
                result.termMonths(),
                result.annualInterestRate(),
                result.paymentType(),
                result.currency(),
                result.monthlyPayment(),
                result.totalInterest(),
                result.totalPayment(),
                savedSchedule.getId()
        );

        CalculationScenario saved = scenarioRepository.save(scenario);
        return ScenarioResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScenarioResponse> getUserScenarios(UUID userId) {
        return scenarioRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ScenarioResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ScenarioResponse getScenarioById(UUID userId, UUID scenarioId) {
        CalculationScenario scenario = scenarioRepository.findById(scenarioId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found: " + scenarioId));
        return ScenarioResponse.from(scenario);
    }

    @Override
    @Transactional(readOnly = true)
    public ScenarioComparisonResponse compareScenarios(UUID userId, List<UUID> scenarioIds) {
        if (scenarioIds == null || scenarioIds.size() < 2 || scenarioIds.size() > 3) {
            throw new BusinessException(ErrorCode.SCENARIO_COMPARE_COUNT);
        }

        List<CalculationScenario> list = scenarioRepository.findAllByIdInAndUserId(scenarioIds, userId);
        if (list.size() != scenarioIds.size()) {
            throw new ResourceNotFoundException("One or more scenarios were not found for this user.");
        }

        return scenarioComparator.compare(list);
    }

    @Override
    public void deleteScenario(UUID userId, UUID scenarioId) {
        CalculationScenario scenario = scenarioRepository.findById(scenarioId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found: " + scenarioId));
        scenarioRepository.delete(scenario);
    }

    @Override
    @Transactional(readOnly = true)
    public void requestPdfExport(UUID userId, String userEmail, UUID scenarioId) {
        CalculationScenario scenario = scenarioRepository.findById(scenarioId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found: " + scenarioId));

        PdfExportEvent event = new PdfExportEvent(
                userId,
                userEmail,
                scenario.getId(),
                scenario.getTitle(),
                scenario.getMongoScheduleId(),
                Instant.now()
        );

        messageProducer.sendPdfExportEvent(event);
    }
}
