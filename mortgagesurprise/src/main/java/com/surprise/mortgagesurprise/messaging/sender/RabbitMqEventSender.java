package com.surprise.mortgagesurprise.messaging.sender;

import com.surprise.mortgagesurprise.messaging.event.Event;

public interface RabbitMqEventSender<T extends Event> extends MessageSender<T> {
}
