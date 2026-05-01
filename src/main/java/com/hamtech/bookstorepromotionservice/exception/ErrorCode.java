package com.hamtech.bookstorepromotionservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ==== 4xx: Lỗi phía client ====
    BAD_REQUEST(HttpStatus.BAD_REQUEST, 4000, "Yêu cầu không hợp lệ."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 4010, "Chưa xác thực."),
    FORBIDDEN(HttpStatus.FORBIDDEN, 4030, "Không có quyền truy cập."),
    NOT_FOUND(HttpStatus.NOT_FOUND, 4040, "Không tìm thấy tài nguyên."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 4050, "Phương thức HTTP không được hỗ trợ."),
    CONFLICT(HttpStatus.CONFLICT, 4090, "Xung đột dữ liệu."),
    UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_ENTITY, 4220, "Lỗi xác thực dữ liệu."),

    // ==== 5xx: Lỗi phía server ====
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 5000, "Lỗi hệ thống nội bộ."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 5030, "Dịch vụ tạm thời không khả dụng."),

    // --- TOKEN ERRORS ---
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, 2000, "Token đã hết hạn."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, 2004, "Token không hợp lệ."),

    // ==== Lỗi khuyến mãi (PROMOTION) ====
    PROMOTION_NOT_FOUND(HttpStatus.NOT_FOUND, 4001, "Khuyến mãi không tồn tại."),
    PROMOTION_CODE_INVALID(HttpStatus.BAD_REQUEST, 4002, "Mã khuyến mãi không hợp lệ."),
    PROMOTION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, 4003, "Mã khuyến mãi đã hết hạn."),
    PROMOTION_CODE_USED_UP(HttpStatus.BAD_REQUEST, 4004, "Mã khuyến mãi đã hết lượt sử dụng."),
    PROMOTION_CODE_NOT_APPLICABLE(HttpStatus.BAD_REQUEST, 4005, "Mã khuyến mãi không áp dụng cho sản phẩm này."),
    PROMOTION_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, 4006, "Mã khuyến mãi đã tồn tại."),
    PROMOTION_INACTIVE(HttpStatus.BAD_REQUEST, 4007, "Khuyến mãi đang không hoạt động."),

    // ==== Lỗi hệ thống không phân loại ====
    UNCATEGORIZED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, 9999, "Lỗi không xác định"),
    ;

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
