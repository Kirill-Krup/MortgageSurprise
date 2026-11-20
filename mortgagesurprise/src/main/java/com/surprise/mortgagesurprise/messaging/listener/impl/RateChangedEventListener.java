package com.surprise.mortgagesurprise.messaging.listener.impl;

import com.surprise.mortgagesurprise.config.RabbitMQConfig;
import com.surprise.mortgagesurprise.messaging.listener.Listener;
import com.surprise.mortgagesurprise.messaging.event.RateChangedEvent;
import com.surprise.mortgagesurprise.service.ScenarioRecalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateChangedEventListener implements Listener<RateChangedEvent> {

    private final ScenarioRecalculationService recalculationService;

    @Override
    @RabbitListener(queues = RabbitMQConfig.RATE_CHANGED_QUEUE)
    public void listen(RateChangedEvent event) {
        log.info("Received RateChangedEvent: bankOfferId={}, bank={}, oldRate={}, newRate={}",
                event.bankOfferId(), event.bankName(), event.oldRate(), event.newRate());
        recalculationService.processRateChange(event);
    }
}