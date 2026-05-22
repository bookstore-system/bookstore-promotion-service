package com.hamtech.bookstorepromotionservice.model.dto.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookstoreMessageEnvelope {

    String eventId;
    UUID sagaId;
    String correlationId;
    String causationId;
    String type;
    String occurredAt;
    UUID orderId;
    String userId;
    JsonNode payload;
}
