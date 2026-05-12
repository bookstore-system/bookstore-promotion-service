package com.hamtech.bookstorepromotionservice.service.impl;

import com.hamtech.bookstorepromotionservice.client.BookServiceClient;
import com.hamtech.bookstorepromotionservice.client.BookExistsResponse;
import com.hamtech.bookstorepromotionservice.client.BookServiceApiResponse;
import com.hamtech.bookstorepromotionservice.exception.AppException;
import com.hamtech.bookstorepromotionservice.exception.ErrorCode;
import com.hamtech.bookstorepromotionservice.model.dto.request.promotionrequest.CreatePromotionRequest;
import com.hamtech.bookstorepromotionservice.model.dto.request.promotionrequest.UpdatePromotionRequest;
import com.hamtech.bookstorepromotionservice.model.dto.request.promotionrequest.ValidatePromotionCodeRequest;
import com.hamtech.bookstorepromotionservice.model.dto.request.promotionrequest.ApplyPromotionRequest;
import com.hamtech.bookstorepromotionservice.model.dto.response.promotionresponse.ApplyPromotionResponse;
import com.hamtech.bookstorepromotionservice.model.dto.response.promotionresponse.PromotionResponse;
import com.hamtech.bookstorepromotionservice.model.dto.response.promotionresponse.PromotionValidationResponse;
import com.hamtech.bookstorepromotionservice.model.entity.Promotion;
import com.hamtech.bookstorepromotionservice.repository.PromotionRepository;
import com.hamtech.bookstorepromotionservice.service.PromotionService;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromotionServiceImpl implements PromotionService {

    PromotionRepository promotionRepository;
    BookServiceClient bookServiceClient;

    @Override
    @Transactional
    public PromotionResponse createPromotion(CreatePromotionRequest request) {
        // Kiểm tra mã khuyến mãi đã tồn tại chưa
        if (promotionRepository.findByCode(request.getCode().toUpperCase()).isPresent()) {
            throw new AppException(ErrorCode.PROMOTION_CODE_ALREADY_EXISTS);
        }

        Promotion promotion = new Promotion();
        promotion.setName(request.getName());
        promotion.setCode(request.getCode().toUpperCase());
        promotion.setDiscountType(Promotion.DiscountType.PERCENTAGE);
        promotion.setDiscountValue(request.getDiscountPercent());
        promotion.setMinOrderValue(0.0);
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setUsageLimit(request.getUsageLimit());
        promotion.setUsageCount(0);
        promotion.setDescription(request.getDescription());

        // Tự động set status dựa trên startDate và endDate
        LocalDate today = LocalDate.now();
        if (request.getEndDate().isBefore(today)) {
            promotion.setStatus(Promotion.Status.EXPIRED);
        } else if (request.getStartDate().isAfter(today)) {
            promotion.setStatus(Promotion.Status.INACTIVE);
        } else {
            promotion.setStatus(Promotion.Status.ACTIVE);
        }

        // Lưu danh sách ID sách áp dụng (null hoặc rỗng = áp dụng mọi sách)
        List<UUID> applicableIds = request.getApplicableBookIds() != null
                ? request.getApplicableBookIds()
                : new ArrayList<>();
        validateApplicableBookIdsWithBookService(applicableIds);
        promotion.setApplicableBookIds(applicableIds);

        Promotion saved = promotionRepository.save(promotion);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public PromotionResponse updatePromotion(UUID promotionId, UpdatePromotionRequest request) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        if (request.getName() != null) {
            promotion.setName(request.getName());
        }
        if (request.getCode() != null) {
            String newCode = request.getCode().toUpperCase();
            promotionRepository.findByCode(newCode)
                    .ifPresent(existing -> {
                        if (!existing.getPromotionID().equals(promotionId)) {
                            throw new AppException(ErrorCode.PROMOTION_CODE_ALREADY_EXISTS);
                        }
                    });
            promotion.setCode(newCode);
        }
        if (request.getDiscountPercent() != null) {
            promotion.setDiscountValue(request.getDiscountPercent());
        }

        if (request.getStartDate() != null) promotion.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) promotion.setEndDate(request.getEndDate());

        // Cập nhật status dựa trên dates
        LocalDate today = LocalDate.now();
        if (promotion.getEndDate().isBefore(today)) {
            promotion.setStatus(Promotion.Status.EXPIRED);
        } else if (promotion.getStartDate().isAfter(today)) {
            promotion.setStatus(Promotion.Status.INACTIVE);
        } else {
            promotion.setStatus(Promotion.Status.ACTIVE);
        }

        if (request.getUsageLimit() != null) promotion.setUsageLimit(request.getUsageLimit());
        if (request.getDescription() != null) promotion.setDescription(request.getDescription());
        
        if (request.getStatus() != null) {
            try {
                promotion.setStatus(Promotion.Status.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }
        
        if (request.getApplicableBookIds() != null) {
            validateApplicableBookIdsWithBookService(request.getApplicableBookIds());
            promotion.setApplicableBookIds(request.getApplicableBookIds());
        }

        Promotion updated = promotionRepository.save(promotion);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deletePromotion(UUID promotionId) {
        if (!promotionRepository.existsById(promotionId)) {
            throw new AppException(ErrorCode.PROMOTION_NOT_FOUND);
        }
        promotionRepository.deleteById(promotionId);
    }

    @Override
    public PromotionResponse getPromotionById(UUID promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
        return mapToResponse(promotion);
    }

    @Override
    public Page<PromotionResponse> getAllPromotions(Pageable pageable) {
        return promotionRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public List<PromotionResponse> getActivePromotions() {
        LocalDate today = LocalDate.now();
        return promotionRepository.findActivePromotions(today)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromotionResponse> getPromotionsByBookId(UUID bookId) {
        LocalDate today = LocalDate.now();
        return promotionRepository.findActivePromotionsByBook(bookId, today)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PromotionValidationResponse validatePromotionCode(ValidatePromotionCodeRequest request) {
        LocalDate today = LocalDate.now();
        Promotion promotion = promotionRepository.findByCode(request.getCode().toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        // Kiểm tra trạng thái
        if (promotion.getStatus() != Promotion.Status.ACTIVE) {
            return PromotionValidationResponse.builder()
                    .isValid(false)
                    .message("Khuyến mãi hiện không hoạt động")
                    .build();
        }

        // Kiểm tra thời gian
        if (today.isBefore(promotion.getStartDate()) || today.isAfter(promotion.getEndDate())) {
            return PromotionValidationResponse.builder()
                    .isValid(false)
                    .message("Mã khuyến mãi đã hết hạn hoặc chưa có hiệu lực")
                    .build();
        }

        // Kiểm tra số lượt sử dụng
        if (promotion.getUsageCount() >= promotion.getUsageLimit()) {
            return PromotionValidationResponse.builder()
                    .isValid(false)
                    .message("Mã khuyến mãi đã hết lượt sử dụng")
                    .build();
        }

        // Kiểm tra áp dụng cho sách
        if (request.getBookIds() != null && !request.getBookIds().isEmpty()) {
            List<UUID> applicableIds = promotion.getApplicableBookIds();
            if (applicableIds != null && !applicableIds.isEmpty()) {
                boolean isApplicable = request.getBookIds().stream()
                        .anyMatch(applicableIds::contains);
                if (!isApplicable) {
                    return PromotionValidationResponse.builder()
                            .isValid(false)
                            .message("Mã không áp dụng cho sản phẩm này")
                            .build();
                }
            }
        }

        return PromotionValidationResponse.builder()
                .isValid(true)
                .message("Mã khuyến mãi hợp lệ")
                .promotionID(promotion.getPromotionID())
                .code(promotion.getCode())
                .discountPercent(promotion.getDiscountPercent())
                .build();
    }

    @Override
    @Transactional
    public void applyPromotionCode(UUID promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
        promotion.incrementUsageCount();
        promotionRepository.save(promotion);
    }

    @Override
    @Transactional
    public PromotionResponse updatePromotionStatus(UUID promotionId, String status) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
        try {
            promotion.setStatus(Promotion.Status.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return mapToResponse(promotionRepository.save(promotion));
    }

    @Override
    @Transactional
    public ApplyPromotionResponse applyPromotion(ApplyPromotionRequest request) {
        Optional<Promotion> promotionOpt = promotionRepository.findByCode(request.getCode().toUpperCase());
        
        if (promotionOpt.isEmpty()) {
            return ApplyPromotionResponse.builder()
                    .isValid(false)
                    .message("Mã khuyến mãi không tồn tại")
                    .discountAmount(0.0)
                    .finalTotal(request.getOrderTotalBeforeDiscount())
                    .build();
        }

        Promotion promotion = promotionOpt.get();
        LocalDate today = LocalDate.now();

        // Kiểm tra chi tiết thay vì dùng isValid() chung chung
        if (promotion.getStatus() != Promotion.Status.ACTIVE) {
            return ApplyPromotionResponse.builder()
                    .isValid(false)
                    .message("Khuyến mãi hiện không hoạt động")
                    .discountAmount(0.0)
                    .finalTotal(request.getOrderTotalBeforeDiscount())
                    .build();
        }

        if (today.isBefore(promotion.getStartDate()) || today.isAfter(promotion.getEndDate())) {
            return ApplyPromotionResponse.builder()
                    .isValid(false)
                    .message("Mã khuyến mãi đã hết hạn hoặc chưa có hiệu lực")
                    .discountAmount(0.0)
                    .finalTotal(request.getOrderTotalBeforeDiscount())
                    .build();
        }

        if (promotion.getUsageCount() >= promotion.getUsageLimit()) {
            return ApplyPromotionResponse.builder()
                    .isValid(false)
                    .message("Mã khuyến mãi đã hết lượt sử dụng")
                    .discountAmount(0.0)
                    .finalTotal(request.getOrderTotalBeforeDiscount())
                    .build();
        }

        if (request.getOrderTotalBeforeDiscount() < promotion.getMinOrderValue()) {
            return ApplyPromotionResponse.builder()
                    .isValid(false)
                    .message(String.format("Đơn hàng chưa đạt giá trị tối thiểu (%,.0fđ)", promotion.getMinOrderValue()))
                    .discountAmount(0.0)
                    .finalTotal(request.getOrderTotalBeforeDiscount())
                    .build();
        }

        Double discountAmount = promotion.calculateDiscountAmount(request.getOrderTotalBeforeDiscount());
        Double finalTotal = request.getOrderTotalBeforeDiscount() - discountAmount;

        return ApplyPromotionResponse.builder()
                .isValid(true)
                .message("Áp dụng mã khuyến mãi thành công")
                .discountAmount(discountAmount)
                .finalTotal(finalTotal)
                .build();
    }

    /**
     * Gọi book-service khi có ít nhất một ID; null/rỗng nghĩa là không giới hạn sách.
     */
    private void validateApplicableBookIdsWithBookService(List<UUID> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return;
        }
        try {
            List<UUID> invalid = new ArrayList<>();
            for (UUID bookId : bookIds) {
                BookServiceApiResponse<BookExistsResponse> wrapper = bookServiceClient.checkBookExists(bookId);
                BookExistsResponse res = wrapper != null ? wrapper.getData() : null;
                if (res == null || !res.isExists()) {
                    invalid.add(bookId);
                }
            }
            if (!invalid.isEmpty()) {
                throw new AppException(
                        ErrorCode.BOOK_IDS_INVALID,
                        ErrorCode.BOOK_IDS_INVALID.getMessage() + " Các ID không hợp lệ: " + invalid);
            }
        } catch (AppException e) {
            throw e;
        } catch (FeignException e) {
            log.warn("Book service checkBookExists failed: status={} message={}", e.status(), e.getMessage());
            throw new AppException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private PromotionResponse mapToResponse(Promotion promotion) {
        return PromotionResponse.builder()
                .promotionID(promotion.getPromotionID())
                .name(promotion.getName())
                .code(promotion.getCode())
                .discountPercent(promotion.getDiscountPercent())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .description(promotion.getDescription())
                .usageCount(promotion.getUsageCount())
                .usageLimit(promotion.getUsageLimit())
                .status(promotion.getStatus().name())
                .applicableBookIds(promotion.getApplicableBookIds())
                .isValid(promotion.isValid())
                .build();
    }
}

