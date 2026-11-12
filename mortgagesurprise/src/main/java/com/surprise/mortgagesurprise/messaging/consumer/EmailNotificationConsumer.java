package com.surprise.mortgagesurprise.messaging.consumer;

import com.surprise.mortgagesurprise.config.RabbitMQConfig;
import com.surprise.mortgagesurprise.messaging.event.EmailNotificationEvent;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationConsumer.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailNotificationConsumer(JavaMailSender mailSender,
                                     @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @RabbitListener(queues = RabbitMQConfig.EMAIL_NOTIFICATION_QUEUE)
    public void handleEmailNotification(EmailNotificationEvent event) {
        log.info("Processing EmailNotificationEvent for {}: subject='{}', type='{}', hasAttachment={}",
                event.toEmail(), event.subject(), event.templateType(), event.attachmentData() != null);
        try {
            if (event.attachmentData() != null && event.attachmentData().length > 0) {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom(fromAddress);
                helper.setTo(event.toEmail());
                helper.setSubject(event.subject());
                helper.setText(event.body());
                String filename = event.attachmentFilename() != null && !event.attachmentFilename().isBlank()
                        ? event.attachmentFilename()
                        : "payment-schedule.pdf";
                helper.addAttachment(filename, new ByteArrayResource(event.attachmentData()));
                mailSender.send(mimeMessage);
            } else {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(event.toEmail());
                message.setSubject(event.subject());
                message.setText(event.body());
                mailSender.send(message);
            }
            log.info("Email successfully sent to {}", event.toEmail());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", event.toEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send email to " + event.toEmail(), e);
        }
    }
}
