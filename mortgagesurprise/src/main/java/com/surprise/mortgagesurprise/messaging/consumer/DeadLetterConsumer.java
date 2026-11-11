package com.surprise.mortgagesurprise.messaging.consumer;

import com.surprise.mortgagesurprise.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.RATE_CHANGED_DLQ)
    public void processDeadLetter(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        var deathHeader = message.getMessageProperties().getXDeathHeader();
        log.error("Поймано битое сообщение в очереди {}: ", RabbitMQConfig.RATE_CHANGED_DLQ);
        log.error("Тело сообщения: {}", body);
        log.error("Причина падения: {}", deathHeader);
    }
}
