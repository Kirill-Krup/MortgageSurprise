package com.surprise.mortgagesurprise.messaging.consumer;

import com.surprise.mortgagesurprise.config.RabbitMQConfig;
import com.surprise.mortgagesurprise.messaging.event.EmailNotificationEvent;
import com.surprise.mortgagesurprise.messaging.event.PdfExportEvent;
import com.surprise.mortgagesurprise.messaging.producer.MessageProducer;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.repository.mongo.PaymentScheduleMongoRepository;
import com.surprise.mortgagesurprise.service.PdfGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PdfExportConsumer {

    private static final Logger log = LoggerFactory.getLogger(PdfExportConsumer.class);

    private final PaymentScheduleMongoRepository scheduleMongoRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final MessageProducer messageProducer;

    public PdfExportConsumer(PaymentScheduleMongoRepository scheduleMongoRepository,
                             PdfGeneratorService pdfGeneratorService,
                             MessageProducer messageProducer) {
        this.scheduleMongoRepository = scheduleMongoRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.messageProducer = messageProducer;
    }

    @RabbitListener(queues = RabbitMQConfig.PDF_EXPORT_QUEUE)
    public void handlePdfExport(PdfExportEvent event) {
        log.info("Received PdfExportEvent for scenario {}: title={}, scheduleId={}",
                event.scenarioId(), event.scenarioTitle(), event.mongoScheduleId());

        try {
            PaymentScheduleDocument schedule = scheduleMongoRepository.findById(event.mongoScheduleId())
                    .orElseThrow(() -> new IllegalStateException("Payment schedule not found: " + event.mongoScheduleId()));

            byte[] pdfBytes = pdfGeneratorService.generatePaymentSchedulePdf(event.scenarioTitle(), schedule);
            log.info("PDF generated successfully for scenario {}. Size: {} bytes", event.scenarioId(), pdfBytes.length);

            EmailNotificationEvent emailEvent = new EmailNotificationEvent(
                    event.userEmail(),
                    "Ваш PDF-график платежей готов!",
                    String.format("График платежей по сценарию '%s' успешно сформирован (размер: %d КБ).",
                            event.scenarioTitle(), pdfBytes.length / 1024),
                    "PDF_EXPORT_READY",
                    Instant.now()
            );
            messageProducer.sendEmailNotification(emailEvent);

        } catch (Exception e) {
            log.error("Failed to export PDF for scenario {}: {}", event.scenarioId(), e.getMessage(), e);
            throw new RuntimeException("PDF export failed for scenario " + event.scenarioId(), e);
        }
    }
}