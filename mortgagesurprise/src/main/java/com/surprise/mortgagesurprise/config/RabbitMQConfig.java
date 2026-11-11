package com.surprise.mortgagesurprise.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "mortgage.exchange";
    public static final String RATE_CHANGED_QUEUE = "mortgage.rate.changed.queue";
    public static final String RATE_CHANGED_ROUTING_KEY = "mortgage.rate.changed";
    public static final String DEAD_LETTER_EXCHANGE = "mortgage.dlx";
    public static final String RATE_CHANGED_DLQ = "mortgage.rate.changed.dlq";
    public static final String RATE_CHANGED_DLQ_ROUTING_KEY = "mortgage.rate.changed.dead";
    public static final String EMAIL_NOTIFICATION_QUEUE = "mortgage.email.notification.queue";
    public static final String EMAIL_NOTIFICATION_ROUTING_KEY = "mortgage.email.notification";
    public static final String PDF_EXPORT_QUEUE = "mortgage.pdf.queue";
    public static final String PDF_EXPORT_ROUTING_KEY = "mortgage.pdf.export";

    @Bean
    public TopicExchange mortgageExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue rateChangedQueue() {
        return QueueBuilder.durable(RATE_CHANGED_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(RATE_CHANGED_DLQ_ROUTING_KEY)
                .build();
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
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue pdfExportQueue() {
        return QueueBuilder.durable(PDF_EXPORT_QUEUE).build();
    }

    @Bean
    public Binding pdfExportBinding(Queue pdfExportQueue, TopicExchange mortgageExchange) {
        return BindingBuilder.bind(pdfExportQueue)
                .to(mortgageExchange)
                .with(PDF_EXPORT_ROUTING_KEY);
    }

    @Bean
    public Queue rateChangedDlq() {
        return QueueBuilder.durable(RATE_CHANGED_DLQ).build();
    }

    @Bean
    public Binding rateChangedDlqBinding(Queue rateChangedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(rateChangedDlq).to(deadLetterExchange).with(RATE_CHANGED_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
