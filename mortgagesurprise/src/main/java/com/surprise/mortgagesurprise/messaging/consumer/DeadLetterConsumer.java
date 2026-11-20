package com.surprise.mortgagesurprise.messaging.consumer;

import com.surprise.mortgagesurprise.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class DeadLetterConsumer {

    @RabbitListener(queues = RabbitMQConfig.RATE_CHANGED_DLQ)
    public void listen(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        var deathHeader = message.getMessageProperties().getXDeathHeader();
        log.error("Dead-letter message caught in queue {}: ", RabbitMQConfig.RATE_CHANGED_DLQ);
        log.error("Message body: {}", body);
        log.error("Failure reason: {}", deathHeader);
    }
}