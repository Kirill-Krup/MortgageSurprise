package com.surprise.mortgagesurprise.messaging.listener.impl;

import com.surprise.mortgagesurprise.config.RabbitMQConfig;
import com.surprise.mortgagesurprise.messaging.event.EmailNotificationEvent;
import com.surprise.mortgagesurprise.messaging.event.PdfExportEvent;
import com.surprise.mortgagesurprise.messaging.listener.Listener;
import com.surprise.mortgagesurprise.messaging.sender.RabbitMqEventSender;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import com.surprise.mortgagesurprise.service.PaymentScheduleService;
import com.surprise.mortgagesurprise.service.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfExportEventListener implements Listener<PdfExportEvent> {

    private final PaymentScheduleService paymentScheduleService;
    private final PdfGeneratorService pdfGeneratorService;
    private final RabbitMqEventSender<EmailNotificationEvent> messageProducer;

    @Override
    @RabbitListener(queues = RabbitMQConfig.PDF_EXPORT_QUEUE)
    public void listen(PdfExportEvent event) {
        log.info("Received PdfExportEvent for scenario {}: title={}, scheduleId={}",
                event.scenarioId(), event.scenarioTitle(), event.mongoScheduleId());

        try {
            PaymentScheduleDocument schedule = paymentScheduleService.getScheduleById(event.mongoScheduleId());

            byte[] pdfBytes = pdfGeneratorService.generatePaymentSchedulePdf(event.scenarioTitle(), schedule);
            log.info("PDF generated successfully for scenario {}. Size: {} bytes", event.scenarioId(), pdfBytes.length);

            String safeTitle = event.scenarioTitle() != null ? event.scenarioTitle().replaceAll("[\\\\/:*?\"<>|]", "_") : "scenario";
            String attachmentName = "Payment_schedule_" + safeTitle + ".pdf";

            EmailNotificationEvent emailEvent = new EmailNotificationEvent(
                    event.userEmail(),
                    "Your PDF payment schedule is ready: " + event.scenarioTitle(),
                    String.format("Hello!\n\nThe payment schedule for scenario '%s' has been successfully generated (size: %d KB).\nThe PDF file is attached to this email.\n\nBest regards,\nMortgageSurprise Team",
                            event.scenarioTitle(), Math.max(1, pdfBytes.length / 1024)),
                    "PDF_EXPORT_READY",
                    Instant.now(),
                    pdfBytes,
                    attachmentName
            );
            messageProducer.send(emailEvent);

        } catch (Exception e) {
            log.error("Failed to export PDF for scenario {}: {}", event.scenarioId(), e.getMessage(), e);
            throw new RuntimeException("PDF export failed for scenario " + event.scenarioId(), e);
        }
    }
}