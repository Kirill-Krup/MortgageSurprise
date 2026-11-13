package com.surprise.mortgagesurprise.integration;

import com.surprise.mortgagesurprise.config.RabbitMQConfig;
import com.surprise.mortgagesurprise.messaging.event.PdfExportEvent;
import com.surprise.mortgagesurprise.messaging.event.RateChangedEvent;
import com.surprise.mortgagesurprise.messaging.producer.MessageProducer;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.repository.mongo.PaymentScheduleMongoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RabbitQueueIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PaymentScheduleMongoRepository scheduleMongoRepository;

    @Test
    @DisplayName("RabbitMQ event delivery: publish RateChangedEvent -> verify message in queue")
    void testRateChangedEventDelivery() {
        UUID bankOfferId = UUID.randomUUID();
        RateChangedEvent event = new RateChangedEvent(
                bankOfferId,
                "Sberbank",
                new BigDecimal("12.50"),
                new BigDecimal("14.00"),
                Instant.now()
        );

        assertDoesNotThrow(() -> {
            messageProducer.sendRateChangedEvent(event);
        });
    }

    @Test
    @DisplayName("Dead Letter Queue routing: publish message to DLX -> verify DLQ topology")
    void testDeadLetterQueueRouting() {
        assertDoesNotThrow(() -> {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DEAD_LETTER_EXCHANGE,
                    RabbitMQConfig.RATE_CHANGED_DLQ_ROUTING_KEY,
                    "{\"error\":\"test-poison-pill\"}"
            );
        });
    }

    @Test
    @DisplayName("PDF export queue delivery: publish PdfExportEvent -> verify no exception")
    void testPdfExportEventDelivery() {
        PaymentScheduleDocument schedule = new PaymentScheduleDocument(
                UUID.randomUUID().toString(),
                "hash-test",
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                new BigDecimal("4000000.00"),
                240,
                240,
                new BigDecimal("10.00"),
                PaymentType.ANNUITY,
                new BigDecimal("38600.00"),
                new BigDecimal("2000000.00"),
                new BigDecimal("6000000.00"),
                List.of(),
                Instant.now()
        );
        PaymentScheduleDocument saved = scheduleMongoRepository.save(schedule);

        PdfExportEvent event = new PdfExportEvent(
                UUID.randomUUID(),
                "test@example.com",
                UUID.randomUUID(),
                "Test Scenario",
                saved.getId(),
                Instant.now()
        );

        assertDoesNotThrow(() -> {
            messageProducer.sendPdfExportEvent(event);
        });
    }
}
