package com.hamtech.bookstorepromotionservice.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "promotion_reservations",
        uniqueConstraints = @UniqueConstraint(name = "uk_promotion_reservation_saga", columnNames = "saga_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionReservation {

    @Id
    @UuidGenerator
    UUID reservationId;

    @Column(name = "saga_id", nullable = false, updatable = false)
    UUID sagaId;

    @Column(name = "order_id")
    UUID orderId;

    @Column(name = "promotion_id", nullable = false)
    UUID promotionId;

    @Column(name = "promotion_code", nullable = false)
    String promotionCode;

    @Column(name = "order_total_before_discount", nullable = false)
    Double orderTotalBeforeDiscount;

    @Column(name = "discount_amount", nullable = false)
    Double discountAmount;

    @Column(name = "final_total", nullable = false)
    Double finalTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Status status;

    @ElementCollection
    @CollectionTable(
            name = "promotion_reservation_book_ids",
            joinColumns = @JoinColumn(name = "reservation_id"))
    @Column(name = "book_id")
    @Builder.Default
    List<UUID> bookIds = new ArrayList<>();

    @Column(name = "failure_reason")
    String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum Status {
        RESERVED,
        CONFIRMED,
        RELEASED
    }
}
