package com.hamtech.bookstorepromotionservice.messaging;

import com.hamtech.bookstorepromotionservice.model.dto.messaging.PromotionCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PromotionCreatedEventListener {

    private final PromotionEventPublisher promotionEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPromotionCreated(PromotionCreatedEvent event) {
        promotionEventPublisher.publishPromotionCreated(event);
    }
}

