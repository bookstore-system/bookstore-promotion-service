package com.hamtech.bookstorepromotionservice.model.dto.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionCreatedEvent {
    private UUID promotionId;
    private String code;
    private String name;
    private String description;
    private Double discountValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}

