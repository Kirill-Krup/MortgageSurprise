package com.surprise.mortgagesurprise.messaging.sender.impl;

import com.surprise.mortgagesurprise.config.RabbitMQConfig;
import com.surprise.mortgagesurprise.messaging.event.RateChangedEvent;
import com.surprise.mortgagesurprise.messaging.sender.RabbitMqEventSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateChangedEventSender implements RabbitMqEventSender<RateChangedEvent> {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void send(RateChangedEvent event) {
        log.info("Publishing RateChangedEvent to exchange {}: {}", RabbitMQConfig.EXCHANGE_NAME, event);
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.RATE_CHANGED_ROUTING_KEY,
                    event
            );
        } catch (Exception e) {
            log.error("Failed to publish RateChangedEvent: {}", e.getMessage(), e);
        }
    }
}