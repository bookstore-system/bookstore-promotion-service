package com.hamtech.bookstorepromotionservice.model.dto.messaging;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionFailedEventPayload {

    String reason;
    String promotionCode;
}
