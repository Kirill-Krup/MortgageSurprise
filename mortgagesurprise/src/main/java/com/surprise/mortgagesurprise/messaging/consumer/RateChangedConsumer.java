package com.surprise.mortgagesurprise.messaging.consumer;

import com.surprise.mortgagesurprise.config.RabbitMQConfig;
import com.surprise.mortgagesurprise.messaging.event.RateChangedEvent;
import com.surprise.mortgagesurprise.service.ScenarioRecalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Single responsibility: Listen to RabbitMQ rate change events and delegate to ScenarioRecalculationService.
 */
@Component
public class RateChangedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RateChangedConsumer.class);
    private final ScenarioRecalculationService recalculationService;

    public RateChangedConsumer(ScenarioRecalculationService recalculationService) {
        this.recalculationService = recalculationService;
    }

    @RabbitListener(queues = RabbitMQConfig.RATE_CHANGED_QUEUE)
    public void handleRateChanged(RateChangedEvent event) {
        log.info("Received RateChangedEvent: bankOfferId={}, bank={}, oldRate={}, newRate={}",
                event.bankOfferId(), event.bankName(), event.oldRate(), event.newRate());
        recalculationService.processRateChange(event);
    }
}
