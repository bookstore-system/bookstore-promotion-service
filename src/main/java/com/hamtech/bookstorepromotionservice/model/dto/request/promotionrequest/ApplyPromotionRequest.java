package com.hamtech.bookstorepromotionservice.model.dto.request.promotionrequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplyPromotionRequest {
    @NotBlank(message = "Mã khuyến mãi không được trống")
    String code;

    UUID userId;

    @NotNull(message = "Giá trị đơn hàng không được trống")
    Double orderTotalBeforeDiscount;
}
