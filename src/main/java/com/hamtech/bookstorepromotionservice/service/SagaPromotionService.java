package com.hamtech.bookstorepromotionservice.service;

import com.hamtech.bookstorepromotionservice.model.dto.messaging.BookstoreMessageEnvelope;

public interface SagaPromotionService {

    void reserve(BookstoreMessageEnvelope command);

    void confirm(BookstoreMessageEnvelope command);

    void release(BookstoreMessageEnvelope command);
}
