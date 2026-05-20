package com.hamtech.bookstorepromotionservice.model.dto.messaging;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionReservePayload {

    String code;
    String promotionCode;
    Double orderTotalBeforeDiscount;
    List<UUID> bookIds;
}
