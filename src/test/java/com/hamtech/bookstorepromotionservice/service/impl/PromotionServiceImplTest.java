package com.hamtech.bookstorepromotionservice.service.impl;

import com.hamtech.bookstorepromotionservice.client.BookServiceClient;
import com.hamtech.bookstorepromotionservice.client.BookExistsResponse;
import com.hamtech.bookstorepromotionservice.client.BookServiceApiResponse;
import com.hamtech.bookstorepromotionservice.exception.AppException;
import com.hamtech.bookstorepromotionservice.exception.ErrorCode;
import com.hamtech.bookstorepromotionservice.model.dto.request.promotionrequest.CreatePromotionRequest;
import com.hamtech.bookstorepromotionservice.model.dto.request.promotionrequest.UpdatePromotionRequest;
import com.hamtech.bookstorepromotionservice.model.entity.Promotion;
import com.hamtech.bookstorepromotionservice.model.entity.PromotionReservation;
import com.hamtech.bookstorepromotionservice.repository.PromotionRepository;
import com.hamtech.bookstorepromotionservice.repository.PromotionReservationRepository;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    @Mock
    PromotionRepository promotionRepository;

    @Mock
    PromotionReservationRepository reservationRepository;

    @Mock
    BookServiceClient bookServiceClient;

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    PromotionServiceImpl promotionService;

    private CreatePromotionRequest baseCreateRequest(List<UUID> applicableBookIds) {
        return CreatePromotionRequest.builder()
                .name("Tết")
                .code("TET26")
                .discountPercent(10.0)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .usageLimit(100)
                .applicableBookIds(applicableBookIds)
                .build();
    }

    @Test
    void createPromotion_whenApplicableBookIdsEmpty_doesNotCallBookService() {
        when(promotionRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(inv -> inv.getArgument(0));

        promotionService.createPromotion(baseCreateRequest(Collections.emptyList()));

        verifyNoInteractions(bookServiceClient);
        verify(promotionRepository).save(any(Promotion.class));
        verify(applicationEventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void createPromotion_whenInvalidBookIds_throwsAndDoesNotSave() {
        UUID badId = UUID.fromString("123e4567-e89b-12d3-a456-426614174099");
        when(promotionRepository.findByCode(anyString())).thenReturn(Optional.empty());

        BookExistsResponse res = new BookExistsResponse();
        res.setExists(false);
        res.setBookId(badId);
        BookServiceApiResponse<BookExistsResponse> wrapper = new BookServiceApiResponse<>();
        wrapper.setCode(200);
        wrapper.setMessage("Success");
        wrapper.setData(res);
        when(bookServiceClient.checkBookExists(badId)).thenReturn(wrapper);

        assertThatThrownBy(() -> promotionService.createPromotion(baseCreateRequest(List.of(badId))))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getErrorCode()).isEqualTo(ErrorCode.BOOK_IDS_INVALID);
                    assertThat(ae.getMessage()).contains(badId.toString());
                });

        verify(promotionRepository, never()).save(any());
    }

    @Test
    void createPromotion_whenFeignFails_throwsServiceUnavailable() {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174088");
        when(promotionRepository.findByCode(anyString())).thenReturn(Optional.empty());

        Request feignRequest = Request.create(
                Request.HttpMethod.POST,
                "http://localhost/api/v1/books/validate-ids",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8);
        Response feignResponse = Response.builder()
                .status(503)
                .reason("Service Unavailable")
                .request(feignRequest)
                .build();
        when(bookServiceClient.checkBookExists(any())).thenThrow(
                FeignException.errorStatus("BookServiceClient#checkBookExists(UUID)", feignResponse));

        assertThatThrownBy(() -> promotionService.createPromotion(baseCreateRequest(List.of(id))))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SERVICE_UNAVAILABLE));

        verify(promotionRepository, never()).save(any());
    }

    @Test
    void updatePromotion_whenApplicableBookIdsNonEmpty_validatesBeforeSave() {
        UUID promoId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        Promotion existing = new Promotion();
        existing.setPromotionID(promoId);
        existing.setName("Old");
        existing.setCode("OLD");
        existing.setDiscountValue(5.0);
        existing.setDiscountType(Promotion.DiscountType.PERCENTAGE);
        existing.setStartDate(LocalDate.now().minusDays(10));
        existing.setEndDate(LocalDate.now().plusDays(10));
        existing.setUsageLimit(50);
        existing.setUsageCount(0);
        existing.setStatus(Promotion.Status.ACTIVE);

        UUID bookId = UUID.fromString("123e4567-e89b-12d3-a456-426614174077");
        when(promotionRepository.findById(promoId)).thenReturn(Optional.of(existing));

        BookExistsResponse ok = new BookExistsResponse();
        ok.setExists(true);
        ok.setBookId(bookId);
        BookServiceApiResponse<BookExistsResponse> okWrapper = new BookServiceApiResponse<>();
        okWrapper.setCode(200);
        okWrapper.setMessage("Success");
        okWrapper.setData(ok);
        when(bookServiceClient.checkBookExists(bookId)).thenReturn(okWrapper);
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePromotionRequest req = new UpdatePromotionRequest();
        req.setApplicableBookIds(List.of(bookId));

        promotionService.updatePromotion(promoId, req);

        verify(bookServiceClient).checkBookExists(bookId);
        verify(promotionRepository).save(any(Promotion.class));
    }
}
