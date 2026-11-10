package com.surprise.mortgagesurprise.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "mortgage.exchange";
    public static final String RATE_CHANGED_QUEUE = "mortgage.rate.changed.queue";
    public static final String RATE_CHANGED_ROUTING_KEY = "mortgage.rate.changed";

    public static final String EMAIL_NOTIFICATION_QUEUE = "mortgage.email.notification.queue";
    public static final String EMAIL_NOTIFICATION_ROUTING_KEY = "mortgage.email.notification";

    @Bean
    public TopicExchange mortgageExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue rateChangedQueue() {
        return QueueBuilder.durable(RATE_CHANGED_QUEUE).build();
    }

    @Bean
    public Queue emailNotificationQueue() {
        return QueueBuilder.durable(EMAIL_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding rateChangedBinding(Queue rateChangedQueue, TopicExchange mortgageExchange) {
        return BindingBuilder.bind(rateChangedQueue).to(mortgageExchange).with(RATE_CHANGED_ROUTING_KEY);
    }

    @Bean
    public Binding emailNotificationBinding(Queue emailNotificationQueue, TopicExchange mortgageExchange) {
        return BindingBuilder.bind(emailNotificationQueue).to(mortgageExchange).with(EMAIL_NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
