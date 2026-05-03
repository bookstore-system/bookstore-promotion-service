package com.hamtech.bookstorepromotionservice.model.dto.response.promotionresponse;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplyPromotionResponse {
    Boolean isValid;
    String message;
    Double discountAmount;
    Double finalTotal;
}
