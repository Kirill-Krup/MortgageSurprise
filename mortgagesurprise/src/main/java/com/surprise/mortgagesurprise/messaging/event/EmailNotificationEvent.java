package com.surprise.mortgagesurprise.messaging.event;

import java.io.Serializable;
import java.time.Instant;

public record EmailNotificationEvent(
        String toEmail,
        String subject,
        String body,
        String templateType,
        Instant timestamp,
        byte[] attachmentData,
        String attachmentFilename
) implements Serializable {

    public EmailNotificationEvent(String toEmail, String subject, String body, String templateType, Instant timestamp) {
        this(toEmail, subject, body, templateType, timestamp, null, null);
    }
}
