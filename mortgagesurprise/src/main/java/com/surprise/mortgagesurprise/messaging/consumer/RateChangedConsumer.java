package com.surprise.mortgagesurprise.messaging.consumer;

import com.surprise.mortgagesurprise.config.RabbitMQConfig;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.messaging.event.EmailNotificationEvent;
import com.surprise.mortgagesurprise.messaging.event.RateChangedEvent;
import com.surprise.mortgagesurprise.messaging.producer.MessageProducer;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.model.entity.CalculationScenario;
import com.surprise.mortgagesurprise.repository.jpa.CalculationScenarioRepository;
import com.surprise.mortgagesurprise.repository.mongo.PaymentScheduleMongoRepository;
import com.surprise.mortgagesurprise.service.MortgageCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class RateChangedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RateChangedConsumer.class);
    private final MessageProducer messageProducer;
    private final MortgageCalculator mortgageCalculator;
    private final PaymentScheduleMongoRepository paymentScheduleMongoRepository;
    private final CalculationScenarioRepository calculationScenarioRepository;

    public RateChangedConsumer(MessageProducer messageProducer,
                               MortgageCalculator mortgageCalculator,
                               PaymentScheduleMongoRepository paymentScheduleMongoRepository,
                               CalculationScenarioRepository calculationScenarioRepository) {
        this.messageProducer = messageProducer;
        this.mortgageCalculator = mortgageCalculator;
        this.paymentScheduleMongoRepository = paymentScheduleMongoRepository;
        this.calculationScenarioRepository = calculationScenarioRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.RATE_CHANGED_QUEUE)
    public void handleRateChanged(RateChangedEvent event) {
        log.info("Received RateChangedEvent: bankOfferId={}, bank={}, oldRate={}, newRate={}",
                event.bankOfferId(), event.bankName(), event.oldRate(), event.newRate());
        List<CalculationScenario> affectedScenario =
                calculationScenarioRepository.findByAnnualInterestRate(event.oldRate());
        if (affectedScenario.isEmpty()) {
            log.info("Affected scenario is empty");
            return;
        }
        log.info("Affected scenario size is {}", affectedScenario.size());
        for(CalculationScenario scenario : affectedScenario){
            try {
                CalculationRequest calcRequest = new CalculationRequest(
                        scenario.getPropertyPrice(),
                        scenario.getDownPayment(),
                        scenario.getTermMonths(),
                        event.newRate(),
                        scenario.getPaymentType(),
                        null,
                        List.of()
                );
                CalculationResult result = mortgageCalculator.calculate(calcRequest);
                PaymentScheduleDocument document = new PaymentScheduleDocument(
                        UUID.randomUUID().toString(),
                        null,
                        result.propertyPrice(),
                        result.downPayment(),
                        result.loanAmount(),
                        result.termMonths(),
                        result.actualTermMonths(),
                        result.annualInterestRate(),
                        result.paymentType(),
                        result.monthlyPayment(),
                        result.totalInterest(),
                        result.totalPayment(),
                        result.schedule(),
                        Instant.now()
                );
                PaymentScheduleDocument savedScheduleDocument = paymentScheduleMongoRepository.save(document);
                var oldPayment = scenario.getMonthlyPayment();
                scenario.setAnnualInterestRate(event.newRate());
                scenario.setMonthlyPayment(result.monthlyPayment());
                scenario.setTotalPayment(result.totalPayment());
                scenario.setTotalInterest(result.totalInterest());
                scenario.setMongoScheduleId(savedScheduleDocument.getId());
                calculationScenarioRepository.save(scenario);
                EmailNotificationEvent notificationEvent = new EmailNotificationEvent(
                        "user-" + scenario.getUserId() + "@example.com",
                        "Ставка обновилась! Сценарий '" + scenario.getTitle() + "' пересчитан",
                        String.format("Банк %s изменил ставку с %s%% на %s%%. " +
                                        "Ваш ежемесячный платёж обновлён с %s руб на %s руб",
                                event.bankName(), event.oldRate(), event.newRate(),
                                oldPayment, result.monthlyPayment()),
                        "RATE_UPDATE_ALERT",
                        Instant.now()
                );
                messageProducer.sendEmailNotification(notificationEvent);
            } catch (Exception e) {
                log.error("Error occurred while recalculating scenario {}: {}", scenario.getId(), e.getMessage());
                throw new RuntimeException("Failed to recalculate scenario " + scenario.getId(), e);
            }
        }
    }
}
