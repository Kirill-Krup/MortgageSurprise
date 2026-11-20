package com.surprise.mortgagesurprise.messaging.listener;

import com.surprise.mortgagesurprise.messaging.event.Event;

public interface Listener<T extends Event> {
    void listen(T t);
}