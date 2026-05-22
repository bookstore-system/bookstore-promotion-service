package com.hamtech.bookstorepromotionservice.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamtech.bookstorepromotionservice.model.dto.messaging.BookstoreMessageEnvelope;
import com.hamtech.bookstorepromotionservice.model.dto.messaging.PromotionReservePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class PromotionCommandParser {

    private final ObjectMapper objectMapper;

    public BookstoreMessageEnvelope parseEnvelope(byte[] body) throws IOException {
        return objectMapper.readValue(body, BookstoreMessageEnvelope.class);
    }

    public PromotionReservePayload parseReservePayload(BookstoreMessageEnvelope envelope) throws IOException {
        JsonNode payload = envelope.getPayload();
        if (payload == null || payload.isNull()) {
            return PromotionReservePayload.builder().build();
        }
        return objectMapper.treeToValue(payload, PromotionReservePayload.class);
    }
}
