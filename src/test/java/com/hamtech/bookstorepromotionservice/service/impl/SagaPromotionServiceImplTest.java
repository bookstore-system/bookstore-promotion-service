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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaPromotionServiceImplTest {

    @Mock
    PromotionRepository promotionRepository;

    @Mock
    PromotionReservationRepository reservationRepository;

    @Mock
    PromotionEventPublisher eventPublisher;

    @Mock
    PromotionCommandParser commandParser;

    @InjectMocks
    SagaPromotionServiceImpl sagaPromotionService;

    UUID sagaId;
    UUID orderId;
    BookstoreMessageEnvelope command;

    @BeforeEach
    void setUp() {
        sagaId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        orderId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        command = BookstoreMessageEnvelope.builder()
                .eventId("evt-1")
                .sagaId(sagaId)
                .orderId(orderId)
                .build();
    }

    @Test
    void reserve_createsReservationAndPublishesReserved() throws Exception {
        Promotion promotion = activePromotion("SAVE10", 10.0, 100, 0);
        PromotionReservePayload payload = PromotionReservePayload.builder()
                .code("SAVE10")
                .orderTotalBeforeDiscount(200.0)
                .build();

        when(reservationRepository.findBySagaId(sagaId)).thenReturn(Optional.empty());
        when(commandParser.parseReservePayload(command)).thenReturn(payload);
        when(promotionRepository.findByCodeForUpdate("SAVE10")).thenReturn(Optional.of(promotion));
        when(reservationRepository.countByPromotionIdAndStatus(promotion.getPromotionID(), Status.RESERVED))
                .thenReturn(0L);
        when(reservationRepository.save(any(PromotionReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        sagaPromotionService.reserve(command);

        ArgumentCaptor<PromotionReservation> reservationCaptor = ArgumentCaptor.forClass(PromotionReservation.class);
        verify(reservationRepository).save(reservationCaptor.capture());
        assertThat(reservationCaptor.getValue().getStatus()).isEqualTo(Status.RESERVED);
        assertThat(reservationCaptor.getValue().getFinalTotal()).isEqualTo(180.0);

        verify(eventPublisher).publish(eq(command), eq(PromotionRoutingKeys.RESERVED_EVENT), any(PromotionReservedEventPayload.class));
        verify(eventPublisher, never()).publish(eq(command), eq(PromotionRoutingKeys.FAILED_EVENT), any());
    }

    @Test
    void reserve_whenLimitReached_publishesFailed() throws Exception {
        Promotion promotion = activePromotion("SAVE10", 10.0, 1, 1);
        PromotionReservePayload payload = PromotionReservePayload.builder()
                .code("SAVE10")
                .orderTotalBeforeDiscount(200.0)
                .build();

        when(reservationRepository.findBySagaId(sagaId)).thenReturn(Optional.empty());
        when(commandParser.parseReservePayload(command)).thenReturn(payload);
        when(promotionRepository.findByCodeForUpdate("SAVE10")).thenReturn(Optional.of(promotion));
        when(reservationRepository.countByPromotionIdAndStatus(promotion.getPromotionID(), Status.RESERVED))
                .thenReturn(0L);

        sagaPromotionService.reserve(command);

        verify(reservationRepository, never()).save(any());
        verify(eventPublisher).publish(eq(command), eq(PromotionRoutingKeys.FAILED_EVENT), any(PromotionFailedEventPayload.class));
    }

    @Test
    void reserve_idempotentWhenAlreadyReserved_republishesReserved() {
        PromotionReservation existing = PromotionReservation.builder()
                .sagaId(sagaId)
                .promotionCode("SAVE10")
                .discountAmount(20.0)
                .finalTotal(180.0)
                .orderTotalBeforeDiscount(200.0)
                .status(Status.RESERVED)
                .build();

        when(reservationRepository.findBySagaId(sagaId)).thenReturn(Optional.of(existing));

        sagaPromotionService.reserve(command);

        verify(eventPublisher).publish(eq(command), eq(PromotionRoutingKeys.RESERVED_EVENT), any(PromotionReservedEventPayload.class));
        verify(promotionRepository, never()).findByCodeForUpdate(any());
    }

    @Test
    void confirm_incrementsUsageAndPublishesConfirmed() {
        UUID promotionId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Promotion promotion = activePromotion("SAVE10", 10.0, 100, 0);
        promotion.setPromotionID(promotionId);

        PromotionReservation reservation = PromotionReservation.builder()
                .sagaId(sagaId)
                .promotionId(promotionId)
                .promotionCode("SAVE10")
                .discountAmount(20.0)
                .finalTotal(180.0)
                .orderTotalBeforeDiscount(200.0)
                .status(Status.RESERVED)
                .build();

        when(reservationRepository.findBySagaId(sagaId)).thenReturn(Optional.of(reservation));
        when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promotion));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sagaPromotionService.confirm(command);

        assertThat(promotion.getUsageCount()).isEqualTo(1);
        assertThat(reservation.getStatus()).isEqualTo(Status.CONFIRMED);
        verify(eventPublisher).publish(eq(command), eq(PromotionRoutingKeys.CONFIRMED_EVENT), any(PromotionReservedEventPayload.class));
    }

    @Test
    void release_marksReservationReleased() {
        PromotionReservation reservation = PromotionReservation.builder()
                .sagaId(sagaId)
                .promotionCode("SAVE10")
                .status(Status.RESERVED)
                .build();

        when(reservationRepository.findBySagaId(sagaId)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sagaPromotionService.release(command);

        assertThat(reservation.getStatus()).isEqualTo(Status.RELEASED);
        verify(eventPublisher).publish(eq(command), eq(PromotionRoutingKeys.RELEASED_EVENT), any(PromotionReservedEventPayload.class));
    }

    private Promotion activePromotion(String code, double percent, int usageLimit, int usageCount) {
        Promotion promotion = new Promotion();
        promotion.setPromotionID(UUID.randomUUID());
        promotion.setCode(code);
        promotion.setName(code);
        promotion.setDiscountType(Promotion.DiscountType.PERCENTAGE);
        promotion.setDiscountValue(percent);
        promotion.setMinOrderValue(0.0);
        promotion.setStartDate(LocalDate.now().minusDays(1));
        promotion.setEndDate(LocalDate.now().plusDays(30));
        promotion.setUsageLimit(usageLimit);
        promotion.setUsageCount(usageCount);
        promotion.setStatus(Promotion.Status.ACTIVE);
        return promotion;
    }
}
