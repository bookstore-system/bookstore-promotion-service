package com.hamtech.bookstorepromotionservice.service.impl;

import com.hamtech.bookstorepromotionservice.messaging.PromotionCommandParser;
import com.hamtech.bookstorepromotionservice.messaging.PromotionEventPublisher;
import com.hamtech.bookstorepromotionservice.messaging.PromotionRoutingKeys;
import com.hamtech.bookstorepromotionservice.model.dto.messaging.BookstoreMessageEnvelope;
import com.hamtech.bookstorepromotionservice.model.dto.messaging.PromotionFailedEventPayload;
import com.hamtech.bookstorepromotionservice.model.dto.messaging.PromotionReservePayload;
import com.hamtech.bookstorepromotionservice.model.dto.messaging.PromotionReservedEventPayload;
import com.hamtech.bookstorepromotionservice.model.entity.Promotion;
import com.hamtech.bookstorepromotionservice.model.entity.PromotionReservation;
import com.hamtech.bookstorepromotionservice.model.entity.PromotionReservation.Status;
import com.hamtech.bookstorepromotionservice.repository.PromotionRepository;
import com.hamtech.bookstorepromotionservice.repository.PromotionReservationRepository;
import com.hamtech.bookstorepromotionservice.service.SagaPromotionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SagaPromotionServiceImpl implements SagaPromotionService {

    PromotionRepository promotionRepository;
    PromotionReservationRepository reservationRepository;
    PromotionEventPublisher eventPublisher;
    PromotionCommandParser commandParser;

    @Override
    @Transactional
    public void reserve(BookstoreMessageEnvelope command) {
        UUID sagaId = requireSagaId(command);
        try {
            PromotionReservePayload payload = commandParser.parseReservePayload(command);
            Optional<PromotionReservation> existing = reservationRepository.findBySagaId(sagaId);
            if (existing.isPresent()) {
                handleReserveIdempotent(command, existing.get());
                return;
            }

            String code = resolvePromotionCode(payload);
            if (!StringUtils.hasText(code)) {
                publishFailed(command, null, "Mã khuyến mãi không được trống");
                return;
            }
            if (payload.getOrderTotalBeforeDiscount() == null) {
                publishFailed(command, code, "Giá trị đơn hàng không được trống");
                return;
            }

            Promotion promotion = promotionRepository.findByCodeForUpdate(code.toUpperCase()).orElse(null);
            if (promotion == null) {
                publishFailed(command, code, "Mã khuyến mãi không tồn tại");
                return;
            }

            List<UUID> bookIds = payload.getBookIds() != null ? payload.getBookIds() : Collections.emptyList();
            String validationError = validatePromotion(promotion, payload.getOrderTotalBeforeDiscount(), bookIds, true);
            if (validationError != null) {
                publishFailed(command, code, validationError);
                return;
            }

            double discountAmount = promotion.calculateDiscountAmount(payload.getOrderTotalBeforeDiscount());
            double finalTotal = payload.getOrderTotalBeforeDiscount() - discountAmount;

            PromotionReservation reservation = PromotionReservation.builder()
                    .sagaId(sagaId)
                    .orderId(command.getOrderId())
                    .promotionId(promotion.getPromotionID())
                    .promotionCode(promotion.getCode())
                    .orderTotalBeforeDiscount(payload.getOrderTotalBeforeDiscount())
                    .discountAmount(discountAmount)
                    .finalTotal(finalTotal)
                    .status(Status.RESERVED)
                    .bookIds(bookIds)
                    .build();
            reservationRepository.save(reservation);

            log.info("Promotion reserved: sagaId={}, orderId={}, code={}, finalTotal={}",
                    sagaId, command.getOrderId(), promotion.getCode(), finalTotal);
            publishReserved(command, reservation);
        } catch (Exception ex) {
            log.error("Failed to process promotion.reserve.command for sagaId={}", sagaId, ex);
            throw new IllegalStateException("promotion reserve failed for sagaId=" + sagaId, ex);
        }
    }

    @Override
    @Transactional
    public void confirm(BookstoreMessageEnvelope command) {
        UUID sagaId = requireSagaId(command);
        Optional<PromotionReservation> existing = reservationRepository.findBySagaId(sagaId);
        if (existing.isEmpty()) {
            publishFailed(command, null, "Không tìm thấy reservation cho saga");
            return;
        }

        PromotionReservation reservation = existing.get();
        if (reservation.getStatus() == Status.CONFIRMED) {
            log.info("Idempotent promotion confirm for sagaId={}", sagaId);
            publishConfirmed(command, reservation);
            return;
        }
        if (reservation.getStatus() == Status.RELEASED) {
            publishFailed(command, reservation.getPromotionCode(), "Reservation đã được giải phóng, không thể confirm");
            return;
        }

        Promotion promotion = promotionRepository.findById(reservation.getPromotionId())
                .orElseThrow(() -> new IllegalStateException("Promotion not found: " + reservation.getPromotionId()));
        promotion.incrementUsageCount();
        promotionRepository.save(promotion);

        reservation.setStatus(Status.CONFIRMED);
        reservationRepository.save(reservation);

        log.info("Promotion confirmed: sagaId={}, orderId={}, code={}",
                sagaId, command.getOrderId(), reservation.getPromotionCode());
        publishConfirmed(command, reservation);
    }

    @Override
    @Transactional
    public void release(BookstoreMessageEnvelope command) {
        UUID sagaId = requireSagaId(command);
        Optional<PromotionReservation> existing = reservationRepository.findBySagaId(sagaId);
        if (existing.isEmpty()) {
            log.warn("promotion.release.command without reservation, sagaId={}", sagaId);
            publishReleased(command, null);
            return;
        }

        PromotionReservation reservation = existing.get();
        if (reservation.getStatus() == Status.RELEASED) {
            log.info("Idempotent promotion release for sagaId={}", sagaId);
            publishReleased(command, reservation);
            return;
        }
        if (reservation.getStatus() == Status.CONFIRMED) {
            log.info("promotion.release.command ignored for confirmed reservation, sagaId={}", sagaId);
            publishReleased(command, reservation);
            return;
        }

        reservation.setStatus(Status.RELEASED);
        reservationRepository.save(reservation);

        log.info("Promotion released: sagaId={}, orderId={}, code={}",
                sagaId, command.getOrderId(), reservation.getPromotionCode());
        publishReleased(command, reservation);
    }

    private void handleReserveIdempotent(BookstoreMessageEnvelope command, PromotionReservation reservation) {
        log.info("Idempotent promotion reserve for sagaId={}, status={}", command.getSagaId(), reservation.getStatus());
        switch (reservation.getStatus()) {
            case RESERVED, CONFIRMED -> publishReserved(command, reservation);
            case RELEASED -> publishFailed(command, reservation.getPromotionCode(),
                    "Reservation đã được giải phóng, không thể reserve lại");
            default -> publishFailed(command, reservation.getPromotionCode(), "Trạng thái reservation không hợp lệ");
        }
    }

    private String validatePromotion(
            Promotion promotion,
            Double orderTotal,
            List<UUID> bookIds,
            boolean countActiveReservations) {
        LocalDate today = LocalDate.now();

        if (promotion.getStatus() != Promotion.Status.ACTIVE) {
            return "Khuyến mãi hiện không hoạt động";
        }
        if (today.isBefore(promotion.getStartDate()) || today.isAfter(promotion.getEndDate())) {
            return "Mã khuyến mãi đã hết hạn hoặc chưa có hiệu lực";
        }
        if (orderTotal < promotion.getMinOrderValue()) {
            return String.format("Đơn hàng chưa đạt giá trị tối thiểu (%,.0fđ)", promotion.getMinOrderValue());
        }
        if (!hasAvailableUsage(promotion, countActiveReservations)) {
            return "Mã khuyến mãi đã hết lượt sử dụng";
        }
        if (bookIds != null && !bookIds.isEmpty()) {
            List<UUID> applicableIds = promotion.getApplicableBookIds();
            if (applicableIds != null && !applicableIds.isEmpty()) {
                boolean isApplicable = bookIds.stream().anyMatch(applicableIds::contains);
                if (!isApplicable) {
                    return "Mã không áp dụng cho sản phẩm này";
                }
            }
        }
        return null;
    }

    boolean hasAvailableUsage(Promotion promotion, boolean countActiveReservations) {
        long reserved = countActiveReservations
                ? reservationRepository.countByPromotionIdAndStatus(promotion.getPromotionID(), Status.RESERVED)
                : 0;
        return promotion.getUsageCount() + reserved < promotion.getUsageLimit();
    }

    private String resolvePromotionCode(PromotionReservePayload payload) {
        if (StringUtils.hasText(payload.getCode())) {
            return payload.getCode();
        }
        return payload.getPromotionCode();
    }

    private UUID requireSagaId(BookstoreMessageEnvelope command) {
        if (command.getSagaId() == null) {
            throw new IllegalArgumentException("sagaId is required");
        }
        return command.getSagaId();
    }

    private void publishReserved(BookstoreMessageEnvelope command, PromotionReservation reservation) {
        PromotionReservedEventPayload payload = PromotionReservedEventPayload.builder()
                .promotionId(reservation.getPromotionId())
                .promotionCode(reservation.getPromotionCode())
                .discountAmount(reservation.getDiscountAmount())
                .finalTotal(reservation.getFinalTotal())
                .orderTotalBeforeDiscount(reservation.getOrderTotalBeforeDiscount())
                .build();
        eventPublisher.publish(command, PromotionRoutingKeys.RESERVED_EVENT, payload);
    }

    private void publishConfirmed(BookstoreMessageEnvelope command, PromotionReservation reservation) {
        PromotionReservedEventPayload payload = PromotionReservedEventPayload.builder()
                .promotionId(reservation.getPromotionId())
                .promotionCode(reservation.getPromotionCode())
                .discountAmount(reservation.getDiscountAmount())
                .finalTotal(reservation.getFinalTotal())
                .orderTotalBeforeDiscount(reservation.getOrderTotalBeforeDiscount())
                .build();
        eventPublisher.publish(command, PromotionRoutingKeys.CONFIRMED_EVENT, payload);
    }

    private void publishReleased(BookstoreMessageEnvelope command, PromotionReservation reservation) {
        PromotionReservedEventPayload payload = reservation == null
                ? PromotionReservedEventPayload.builder().build()
                : PromotionReservedEventPayload.builder()
                        .promotionId(reservation.getPromotionId())
                        .promotionCode(reservation.getPromotionCode())
                        .discountAmount(reservation.getDiscountAmount())
                        .finalTotal(reservation.getFinalTotal())
                        .orderTotalBeforeDiscount(reservation.getOrderTotalBeforeDiscount())
                        .build();
        eventPublisher.publish(command, PromotionRoutingKeys.RELEASED_EVENT, payload);
    }

    private void publishFailed(BookstoreMessageEnvelope command, String promotionCode, String reason) {
        log.warn("Promotion saga failed: sagaId={}, orderId={}, reason={}",
                command.getSagaId(), command.getOrderId(), reason);
        PromotionFailedEventPayload payload = PromotionFailedEventPayload.builder()
                .promotionCode(promotionCode)
                .reason(reason)
                .build();
        eventPublisher.publish(command, PromotionRoutingKeys.FAILED_EVENT, payload);
    }
}
