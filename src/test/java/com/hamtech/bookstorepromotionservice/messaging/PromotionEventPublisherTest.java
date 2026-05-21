package com.hamtech.bookstorepromotionservice.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamtech.bookstorepromotionservice.config.RabbitMQConfig;
import com.hamtech.bookstorepromotionservice.model.dto.messaging.PromotionCreatedEvent;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PromotionEventPublisherTest {

    @Test
    void publishPromotionCreated_sendsRawJsonWithoutJavaTypeHeader() throws Exception {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        PromotionEventPublisher publisher = new PromotionEventPublisher(rabbitTemplate, objectMapper);
        UUID promotionId = UUID.randomUUID();
        PromotionCreatedEvent event = PromotionCreatedEvent.builder()
                .promotionId(promotionId)
                .code("PROMO10")
                .name("Promo")
                .description("Promotion")
                .discountValue(10.0)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .status("ACTIVE")
                .build();

        publisher.publishPromotionCreated(event);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(
                eq(RabbitMQConfig.EVENTS_EXCHANGE),
                eq(PromotionRoutingKeys.PROMOTION_CREATED_EVENT),
                messageCaptor.capture());

        Message message = messageCaptor.getValue();
        assertThat(message.getMessageProperties().getContentType())
                .isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat(message.getMessageProperties().getHeaders()).doesNotContainKey("__TypeId__");

        JsonNode body = objectMapper.readTree(message.getBody());
        assertThat(body.get("promotionId").asText()).isEqualTo(promotionId.toString());
        assertThat(body.get("code").asText()).isEqualTo("PROMO10");
        assertThat(body.get("status").asText()).isEqualTo("ACTIVE");
    }
}
