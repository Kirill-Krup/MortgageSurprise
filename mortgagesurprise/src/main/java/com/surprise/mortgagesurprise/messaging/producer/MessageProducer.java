package com.surprise.mortgagesurprise.messaging.producer;

import com.surprise.mortgagesurprise.messaging.event.EmailNotificationEvent;
import com.surprise.mortgagesurprise.messaging.event.PdfExportEvent;
import com.surprise.mortgagesurprise.messaging.event.RateChangedEvent;

public interface MessageProducer {
    void sendRateChangedEvent(RateChangedEvent event);
    void sendEmailNotification(EmailNotificationEvent event);
    void sendPdfExportEvent(PdfExportEvent event);
}
