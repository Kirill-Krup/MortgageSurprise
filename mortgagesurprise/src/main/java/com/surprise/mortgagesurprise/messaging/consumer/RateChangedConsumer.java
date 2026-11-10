package com.surprise.mortgagesurprise.messaging.consumer;

import com.surprise.mortgagesurprise.config.RabbitMQConfig;
import com.surprise.mortgagesurprise.messaging.event.RateChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RateChangedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RateChangedConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.RATE_CHANGED_QUEUE)
    public void handleRateChanged(RateChangedEvent event) {
        log.info("Received RateChangedEvent: bankOfferId={}, bank={}, oldRate={}, newRate={}",
                event.bankOfferId(), event.bankName(), event.oldRate(), event.newRate());
    }
}
