package com.hamtech.bookstorepromotionservice.config;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamtech.bookstorepromotionservice.messaging.PromotionRoutingKeys;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String COMMANDS_EXCHANGE = "bookstore.commands";
    public static final String EVENTS_EXCHANGE = "bookstore.events";
    public static final String PROMOTION_COMMANDS_QUEUE = "promotion.commands.queue";
    public static final String PROMOTION_COMMANDS_DLQ = "promotion.commands.dlq";

    @Bean
    public TopicExchange bookstoreCommandsExchange() {
        return new TopicExchange(COMMANDS_EXCHANGE);
    }

    @Bean
    public TopicExchange bookstoreEventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE);
    }

    @Bean
    public Queue promotionCommandsQueue() {
        return QueueBuilder.durable(PROMOTION_COMMANDS_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", PROMOTION_COMMANDS_DLQ)
                .build();
    }

    @Bean
    public Queue promotionCommandsDlq() {
        return new Queue(PROMOTION_COMMANDS_DLQ, true);
    }

    @Bean
    public Binding promotionReserveCommandBinding(Queue promotionCommandsQueue, TopicExchange bookstoreCommandsExchange) {
        return BindingBuilder.bind(promotionCommandsQueue)
                .to(bookstoreCommandsExchange)
                .with(PromotionRoutingKeys.RESERVE_COMMAND);
    }

    @Bean
    public Binding promotionConfirmCommandBinding(Queue promotionCommandsQueue, TopicExchange bookstoreCommandsExchange) {
        return BindingBuilder.bind(promotionCommandsQueue)
                .to(bookstoreCommandsExchange)
                .with(PromotionRoutingKeys.CONFIRM_COMMAND);
    }

    @Bean
    public Binding promotionReleaseCommandBinding(Queue promotionCommandsQueue, TopicExchange bookstoreCommandsExchange) {
        return BindingBuilder.bind(promotionCommandsQueue)
                .to(bookstoreCommandsExchange)
                .with(PromotionRoutingKeys.RELEASE_COMMAND);
    }

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper()
                .configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature(), true)
                .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true)
                .findAndRegisterModules();
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory promotionCommandListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
