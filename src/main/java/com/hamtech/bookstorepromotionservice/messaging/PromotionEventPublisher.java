package com.hamtech.bookstorepromotionservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hamtech.bookstorepromotionservice.config.RabbitMQConfig;
import com.hamtech.bookstorepromotionservice.model.dto.messaging.BookstoreMessageEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void publish(BookstoreMessageEnvelope source, String routingKey, Object payload) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("eventId", source.getEventId() != null ? source.getEventId() : UUID.randomUUID().toString());
        if (source.getSagaId() != null) {
            body.put("sagaId", source.getSagaId().toString());
        }
        if (source.getCorrelationId() != null) {
            body.put("correlationId", source.getCorrelationId());
        }
        if (source.getCausationId() != null) {
            body.put("causationId", source.getCausationId());
        }
        body.put("type", routingKey);
        body.put("occurredAt", LocalDateTime.now().toString());
        if (source.getOrderId() != null) {
            body.put("orderId", source.getOrderId().toString());
        }
        if (source.getUserId() != null) {
            body.put("userId", source.getUserId());
        }
        body.set("payload", objectMapper.valueToTree(payload));

        rabbitTemplate.convertAndSend(RabbitMQConfig.EVENTS_EXCHANGE, routingKey, body);
        log.info("Published promotion event: type={}, sagaId={}, orderId={}",
                routingKey, source.getSagaId(), source.getOrderId());
    }
}
