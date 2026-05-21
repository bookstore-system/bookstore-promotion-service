package com.hamtech.bookstorepromotionservice.messaging;

import com.hamtech.bookstorepromotionservice.model.dto.messaging.PromotionCreatedEvent;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PromotionCreatedEventListenerTest {

    @Test
    void onPromotionCreated_publishesPromotionCreatedEvent() {
        PromotionEventPublisher publisher = mock(PromotionEventPublisher.class);
        PromotionCreatedEventListener listener = new PromotionCreatedEventListener(publisher);
        PromotionCreatedEvent event = PromotionCreatedEvent.builder()
                .promotionId(UUID.randomUUID())
                .code("PROMO10")
                .name("Promo")
                .discountValue(10.0)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .status("ACTIVE")
                .build();

        listener.onPromotionCreated(event);

        verify(publisher).publishPromotionCreated(event);
    }
}

