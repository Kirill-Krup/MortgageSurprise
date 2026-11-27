package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.messaging.event.EmailNotificationEvent;
import com.surprise.mortgagesurprise.messaging.event.RateChangedEvent;
import com.surprise.mortgagesurprise.messaging.sender.RabbitMqEventSender;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.model.entity.CalculationScenario;
import com.surprise.mortgagesurprise.repository.jpa.CalculationScenarioRepository;
import com.surprise.mortgagesurprise.service.MortgageCalculator;
import com.surprise.mortgagesurprise.service.PaymentScheduleService;
import com.surprise.mortgagesurprise.service.ScenarioRecalculationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Single responsibility: Execute scenario recalculation and notification when bank interest rates change.
 */
@Service
@RequiredArgsConstructor
public class ScenarioRecalculationServiceImpl implements ScenarioRecalculationService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioRecalculationServiceImpl.class);

    private final CalculationScenarioRepository scenarioRepository;
    private final MortgageCalculator mortgageCalculator;
    private final PaymentScheduleService paymentScheduleService;
    private final RabbitMqEventSender<EmailNotificationEvent> messageProducer;

    @Override
    @Transactional
    public void processRateChange(RateChangedEvent event) {
        log.info("Processing RateChangedEvent: bankOfferId={}, bank={}, oldRate={}, newRate={}",
                event.bankOfferId(), event.bankName(), event.oldRate(), event.newRate());

        List<CalculationScenario> affectedScenarios = scenarioRepository.findByAnnualInterestRate(event.oldRate());
        if (affectedScenarios.isEmpty()) {
            log.info("No affected scenarios found for oldRate={}", event.oldRate());
            return;
        }

        log.info("Found {} affected scenario(s) to recalculate", affectedScenarios.size());
        for (CalculationScenario scenario : affectedScenarios) {
            try {
                recalculateScenario(scenario, event);
            } catch (Exception e) {
                log.error("Error occurred while recalculating scenario {}: {}", scenario.getId(), e.getMessage());
                throw new IllegalStateException("Failed to recalculate scenario " + scenario.getId(), e);
            }
        }
    }

    private void recalculateScenario(CalculationScenario scenario, RateChangedEvent event) {
        CalculationRequest calcRequest = new CalculationRequest(
                scenario.getPropertyPrice(),
                scenario.getDownPayment(),
                scenario.getTermMonths(),
                event.newRate(),
                scenario.getPaymentType(),
                scenario.getCurrency(),
                null,
                List.of()
        );

        CalculationResult result = mortgageCalculator.calculate(calcRequest);
        PaymentScheduleDocument savedSchedule = paymentScheduleService.saveSchedule(result);

        BigDecimal oldPayment = scenario.getMonthlyPayment();
        scenario.setAnnualInterestRate(event.newRate());
        scenario.setMonthlyPayment(result.monthlyPayment());
        scenario.setTotalPayment(result.totalPayment());
        scenario.setTotalInterest(result.totalInterest());
        scenario.setMongoScheduleId(savedSchedule.getId());
        scenarioRepository.save(scenario);

        EmailNotificationEvent notificationEvent = new EmailNotificationEvent(
                "user-" + scenario.getUserId() + "@example.com",
                "Interest rate updated! Scenario '" + scenario.getTitle() + "' recalculated",
                String.format("Bank %s updated interest rate from %s%% to %s%%. " +
                                "Your monthly payment has been updated from %s to %s",
                        event.bankName(), event.oldRate(), event.newRate(),
                        oldPayment, result.monthlyPayment()),
                "RATE_UPDATE_ALERT",
                Instant.now()
        );
        sendAfterCommit(notificationEvent);
    }

    private void sendAfterCommit(EmailNotificationEvent notificationEvent) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messageProducer.send(notificationEvent);
                }
            });
        } else {
            messageProducer.send(notificationEvent);
        }
    }
}
