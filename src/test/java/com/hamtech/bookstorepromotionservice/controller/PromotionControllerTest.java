package com.hamtech.bookstorepromotionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamtech.bookstorepromotionservice.model.dto.request.promotionrequest.ApplyPromotionRequest;
import com.hamtech.bookstorepromotionservice.model.dto.request.promotionrequest.CreatePromotionRequest;
import com.hamtech.bookstorepromotionservice.model.dto.request.promotionrequest.UpdatePromotionRequest;
import com.hamtech.bookstorepromotionservice.model.dto.request.promotionrequest.ValidatePromotionCodeRequest;
import com.hamtech.bookstorepromotionservice.model.dto.response.promotionresponse.ApplyPromotionResponse;
import com.hamtech.bookstorepromotionservice.model.dto.response.promotionresponse.PromotionResponse;
import com.hamtech.bookstorepromotionservice.model.dto.response.promotionresponse.PromotionValidationResponse;
import com.hamtech.bookstorepromotionservice.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PromotionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PromotionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PromotionService promotionService;

    private static PromotionResponse samplePromotion(UUID id) {
        return PromotionResponse.builder()
                .promotionID(id)
                .name("Promo")
                .code("PROMO10")
                .discountPercent(10.0)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .description("desc")
                .usageCount(0)
                .usageLimit(100)
                .status("ACTIVE")
                .applicableBookIds(List.of())
                .isValid(true)
                .build();
    }

    @Test
    void createPromotion_returns200() throws Exception {
        CreatePromotionRequest req = CreatePromotionRequest.builder()
                .name("Promo")
                .code("PROMO10")
                .discountPercent(10.0)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .usageLimit(100)
                .description("desc")
                .applicableBookIds(List.of())
                .build();

        UUID id = UUID.randomUUID();
        when(promotionService.createPromotion(any(CreatePromotionRequest.class)))
                .thenReturn(samplePromotion(id));

        mockMvc.perform(post("/api/v1/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.promotionID").value(id.toString()))
                .andExpect(jsonPath("$.result.code").value("PROMO10"));
    }

    @Test
    void updatePromotion_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UpdatePromotionRequest req = UpdatePromotionRequest.builder()
                .name("Promo2")
                .code("PROMO20")
                .discountPercent(20.0)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .usageLimit(50)
                .description("desc")
                .status("ACTIVE")
                .applicableBookIds(List.of())
                .build();

        when(promotionService.updatePromotion(eq(id), any(UpdatePromotionRequest.class)))
                .thenReturn(samplePromotion(id));

        mockMvc.perform(put("/api/v1/promotions/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.promotionID").value(id.toString()));
    }

    @Test
    void deletePromotion_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(promotionService).deletePromotion(id);

        mockMvc.perform(delete("/api/v1/promotions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void getPromotionById_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(promotionService.getPromotionById(id)).thenReturn(samplePromotion(id));

        mockMvc.perform(get("/api/v1/promotions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.promotionID").value(id.toString()));
    }

    @Test
    void getAllPromotions_returns200_andHasPaginationFields() throws Exception {
        UUID id = UUID.randomUUID();
        Page<PromotionResponse> page = new PageImpl<>(
                List.of(samplePromotion(id)),
                PageRequest.of(0, 10),
                1
        );
        when(promotionService.getAllPromotions(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/promotions")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.content[0].promotionID").value(id.toString()))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(10))
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.totalPages").value(1));
    }

    @Test
    void getActivePromotions_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(promotionService.getActivePromotions()).thenReturn(List.of(samplePromotion(id)));

        mockMvc.perform(get("/api/v1/promotions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].promotionID").value(id.toString()));
    }

    @Test
    void getPromotionsByBookId_returns200() throws Exception {
        UUID bookId = UUID.randomUUID();
        UUID promoId = UUID.randomUUID();
        when(promotionService.getPromotionsByBookId(bookId)).thenReturn(List.of(samplePromotion(promoId)));

        mockMvc.perform(get("/api/v1/promotions/book/{bookId}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].promotionID").value(promoId.toString()));
    }

    @Test
    void validatePromotionCode_valid_returnsCode1000() throws Exception {
        ValidatePromotionCodeRequest req = ValidatePromotionCodeRequest.builder()
                .code("PROMO10")
                .bookIds(List.of(UUID.randomUUID()))
                .build();

        PromotionValidationResponse svcRes = PromotionValidationResponse.builder()
                .isValid(true)
                .message("OK")
                .code("PROMO10")
                .discountPercent(10.0)
                .build();
        when(promotionService.validatePromotionCode(any(ValidatePromotionCodeRequest.class)))
                .thenReturn(svcRes);

        mockMvc.perform(post("/api/v1/promotions/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.isValid").value(true));
    }

    @Test
    void validatePromotionCode_invalid_returnsCode4002() throws Exception {
        ValidatePromotionCodeRequest req = ValidatePromotionCodeRequest.builder()
                .code("PROMO10")
                .build();

        PromotionValidationResponse svcRes = PromotionValidationResponse.builder()
                .isValid(false)
                .message("INVALID")
                .reason("EXPIRED")
                .build();
        when(promotionService.validatePromotionCode(any(ValidatePromotionCodeRequest.class)))
                .thenReturn(svcRes);

        mockMvc.perform(post("/api/v1/promotions/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.result.isValid").value(false));
    }

    @Test
    void updatePromotionStatus_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(promotionService.updatePromotionStatus(id, "ACTIVE")).thenReturn(samplePromotion(id));

        mockMvc.perform(patch("/api/v1/promotions/{id}/status", id)
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.promotionID").value(id.toString()));
    }

    @Test
    void applyPromotion_valid_returnsCode1000() throws Exception {
        ApplyPromotionRequest req = ApplyPromotionRequest.builder()
                .code("PROMO10")
                .userId(UUID.randomUUID())
                .orderTotalBeforeDiscount(100.0)
                .build();

        ApplyPromotionResponse svcRes = ApplyPromotionResponse.builder()
                .isValid(true)
                .message("OK")
                .discountAmount(10.0)
                .finalTotal(90.0)
                .build();
        when(promotionService.applyPromotion(any(ApplyPromotionRequest.class))).thenReturn(svcRes);

        mockMvc.perform(post("/api/v1/promotions/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.isValid").value(true))
                .andExpect(jsonPath("$.result.finalTotal").value(90.0));
    }

    @Test
    void applyPromotion_invalid_returnsCode4002() throws Exception {
        ApplyPromotionRequest req = ApplyPromotionRequest.builder()
                .code("PROMO10")
                .orderTotalBeforeDiscount(100.0)
                .build();

        ApplyPromotionResponse svcRes = ApplyPromotionResponse.builder()
                .isValid(false)
                .message("INVALID")
                .build();
        when(promotionService.applyPromotion(any(ApplyPromotionRequest.class))).thenReturn(svcRes);

        mockMvc.perform(post("/api/v1/promotions/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.result.isValid").value(false));
    }
}

