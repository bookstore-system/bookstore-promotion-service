package com.hamtech.bookstorepromotionservice.messaging.consumer;

import com.hamtech.bookstorepromotionservice.config.RabbitMQConfig;
import com.hamtech.bookstorepromotionservice.messaging.PromotionCommandParser;
import com.hamtech.bookstorepromotionservice.messaging.PromotionRoutingKeys;
import com.hamtech.bookstorepromotionservice.model.dto.messaging.BookstoreMessageEnvelope;
import com.hamtech.bookstorepromotionservice.service.SagaPromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionCommandConsumer {

    private final PromotionCommandParser commandParser;
    private final SagaPromotionService sagaPromotionService;

    @RabbitListener(
            queues = RabbitMQConfig.PROMOTION_COMMANDS_QUEUE,
            containerFactory = "promotionCommandListenerContainerFactory")
    public void handleCommand(Message message) throws Exception {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        BookstoreMessageEnvelope envelope = commandParser.parseEnvelope(message.getBody());
        log.info("Received promotion command: type={}, routingKey={}, sagaId={}, orderId={}",
                envelope.getType(), routingKey, envelope.getSagaId(), envelope.getOrderId());

        if (PromotionRoutingKeys.RESERVE_COMMAND.equals(routingKey)
                || PromotionRoutingKeys.RESERVE_COMMAND.equals(envelope.getType())) {
            sagaPromotionService.reserve(envelope);
            return;
        }
        if (PromotionRoutingKeys.CONFIRM_COMMAND.equals(routingKey)
                || PromotionRoutingKeys.CONFIRM_COMMAND.equals(envelope.getType())) {
            sagaPromotionService.confirm(envelope);
            return;
        }
        if (PromotionRoutingKeys.RELEASE_COMMAND.equals(routingKey)
                || PromotionRoutingKeys.RELEASE_COMMAND.equals(envelope.getType())) {
            sagaPromotionService.release(envelope);
            return;
        }

        log.warn("Ignored unknown promotion command routingKey={}, type={}", routingKey, envelope.getType());
    }
}
