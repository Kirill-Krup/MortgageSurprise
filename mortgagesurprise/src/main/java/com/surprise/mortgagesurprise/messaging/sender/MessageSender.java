package com.surprise.mortgagesurprise.messaging.sender;

public interface MessageSender<T> {
    void send(T event);
}
