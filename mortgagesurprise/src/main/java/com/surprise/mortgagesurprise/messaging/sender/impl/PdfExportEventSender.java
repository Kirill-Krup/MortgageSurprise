package com.surprise.mortgagesurprise.messaging.sender.impl;

import com.surprise.mortgagesurprise.config.RabbitMQConfig;
import com.surprise.mortgagesurprise.messaging.event.PdfExportEvent;
import com.surprise.mortgagesurprise.messaging.sender.RabbitMqEventSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportEventSender implements RabbitMqEventSender<PdfExportEvent> {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void send(PdfExportEvent event) {
        log.info("Publishing PdfExportEvent for scenario {}: {}", event.scenarioId(), event);
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.PDF_EXPORT_ROUTING_KEY,
                    event
            );
        } catch (Exception e) {
            log.error("Failed to publish PdfExportEvent: {}", e.getMessage(), e);
        }
    }
}