package com.surprise.mortgagesurprise.messaging.event;

public sealed interface Event permits EmailNotificationEvent, PdfExportEvent, RateChangedEvent{
}
