package com.hamtech.bookstorepromotionservice.repository;

import com.hamtech.bookstorepromotionservice.model.entity.PromotionReservation;
import com.hamtech.bookstorepromotionservice.model.entity.PromotionReservation.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionReservationRepository extends JpaRepository<PromotionReservation, UUID> {

    Optional<PromotionReservation> findBySagaId(UUID sagaId);

    long countByPromotionIdAndStatus(UUID promotionId, Status status);
}
