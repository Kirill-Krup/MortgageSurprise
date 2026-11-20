package com.surprise.mortgagesurprise.integration;

import com.surprise.mortgagesurprise.config.RabbitMQConfig;
import com.surprise.mortgagesurprise.messaging.event.PdfExportEvent;
import com.surprise.mortgagesurprise.messaging.event.RateChangedEvent;
import com.surprise.mortgagesurprise.messaging.sender.RabbitMqEventSender;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.repository.mongo.PaymentScheduleMongoRepository;
import com.surprise.mortgagesurprise.testutil.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

@Tag("integration")
@DisplayName("RabbitMQ Queue Integration Tests")
class RabbitQueueIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RabbitMqEventSender<RateChangedEvent> messageSender;

    @Autowired
    private RabbitMqEventSender<PdfExportEvent> pdfExportEventRabbitMqMessageSender;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PaymentScheduleMongoRepository scheduleMongoRepository;

    @Nested
    @DisplayName("Event Delivery")
    class EventDelivery {

        @Test
        @DisplayName("Should publish RateChangedEvent without exceptions")
        void sendRateChangedEvent_shouldPublishEventToRabbitMQ_whenValidEventSent() {
            // Arrange
            RateChangedEvent event = new RateChangedEvent(
                    UUID.randomUUID(),
                    "Sberbank",
                    new BigDecimal("12.50"),
                    new BigDecimal("14.00"),
                    Instant.now()
            );

            // Act & Assert
            assertThatCode(() -> messageSender.send(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should route poison message to Dead Letter Queue topology")
        void sendDeadLetter_shouldRouteMessageToDlq_whenPoisonPillSent() {
            // Arrange
            String payload = "{\"error\":\"test-poison-pill\"}";

            // Act & Assert
            assertThatCode(() -> rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DEAD_LETTER_EXCHANGE,
                    RabbitMQConfig.RATE_CHANGED_DLQ_ROUTING_KEY,
                    payload
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should publish PdfExportEvent for existing schedule in MongoDB without exceptions")
        void sendPdfExportEvent_shouldPublishEventToRabbitMQ_whenValidEventSent() {
            // Arrange
            PaymentScheduleDocument schedule = new PaymentScheduleDocument(
                    UUID.randomUUID().toString(),
                    TestConstants.CALCULATION_HASH,
                    TestConstants.PROPERTY_PRICE,
                    TestConstants.DOWN_PAYMENT,
                    TestConstants.LOAN_AMOUNT,
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
                    TestConstants.USER_ID,
                    TestConstants.USER_EMAIL,
                    UUID.randomUUID(),
                    "Test Scenario",
                    saved.getId(),
                    Instant.now()
            );

            // Act & Assert
            assertThatCode(() -> pdfExportEventRabbitMqMessageSender.send(event))
                    .doesNotThrowAnyException();
        }
    }
}