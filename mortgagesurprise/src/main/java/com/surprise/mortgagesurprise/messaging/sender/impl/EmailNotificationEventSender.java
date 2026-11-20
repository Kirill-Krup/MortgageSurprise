package com.surprise.mortgagesurprise.messaging.sender.impl;

import com.surprise.mortgagesurprise.config.RabbitMQConfig;
import com.surprise.mortgagesurprise.messaging.event.EmailNotificationEvent;
import com.surprise.mortgagesurprise.messaging.sender.RabbitMqEventSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationEventSender implements RabbitMqEventSender<EmailNotificationEvent> {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void send(EmailNotificationEvent event) {
        log.info("Publishing EmailNotificationEvent: {}", event);
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.EMAIL_NOTIFICATION_ROUTING_KEY,
                    event
            );
        } catch (Exception e) {
            log.error("Failed to publish EmailNotificationEvent: {}", e.getMessage(), e);
        }
    }
}