package com.surprise.mortgagesurprise.messaging.producer;

import com.surprise.mortgagesurprise.config.RabbitMQConfig;
import com.surprise.mortgagesurprise.messaging.event.EmailNotificationEvent;
import com.surprise.mortgagesurprise.messaging.event.RateChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitMessageProducer implements MessageProducer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMessageProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void sendRateChangedEvent(RateChangedEvent event) {
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

    @Override
    public void sendEmailNotification(EmailNotificationEvent event) {
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
