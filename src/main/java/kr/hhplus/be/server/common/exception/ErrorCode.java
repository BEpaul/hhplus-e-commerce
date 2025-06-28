package kr.hhplus.be.server.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // 400 Bad Request - 잘못된 요청
    INVALID_INPUT_VALUE("INVALID_INPUT_VALUE", HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    NEGATIVE_CHARGE_POINT("NEGATIVE_CHARGE_POINT", HttpStatus.BAD_REQUEST, "충전 금액은 0보다 커야 합니다."),
    NEGATIVE_USE_POINT("NEGATIVE_USE_POINT", HttpStatus.BAD_REQUEST, "사용 금액은 0보다 커야 합니다."),
    ORDER_PRODUCT_EMPTY("ORDER_PRODUCT_EMPTY", HttpStatus.BAD_REQUEST, "주문 상품이 비어있습니다."),
    NOT_SUPPORTED_DISCOUNT_TYPE("NOT_SUPPORTED_DISCOUNT_TYPE", HttpStatus.BAD_REQUEST, "지원하지 않는 할인 타입입니다."),
    
    // 404 Not Found - 리소스를 찾을 수 없음
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND, "상품이 존재하지 않습니다."),
    COUPON_NOT_FOUND("COUPON_NOT_FOUND", HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."),
    BESTSELLER_NOT_FOUND("BESTSELLER_NOT_FOUND", HttpStatus.NOT_FOUND, "베스트셀러를 찾을 수 없습니다."),
    PAYMENT_INFO_NOT_EXIST("PAYMENT_INFO_NOT_EXIST", HttpStatus.NOT_FOUND, "결제 정보가 없습니다."),
    
    // 409 Conflict - 충돌 (중복, 이미 사용됨 등)
    DUPLICATE_PAYMENT("DUPLICATE_PAYMENT", HttpStatus.CONFLICT, "이미 처리된 결제 요청입니다."),
    ALREADY_USED_COUPON("ALREADY_USED_COUPON", HttpStatus.CONFLICT, "이미 사용된 쿠폰입니다."),
    ALREADY_APPLIED_COUPON("ALREADY_APPLIED_COUPON", HttpStatus.CONFLICT, "이미 적용된 쿠폰입니다."),
    NOT_OWNED_USER_COUPON("NOT_OWNED_USER_COUPON", HttpStatus.CONFLICT, "소유하지 않은 쿠폰입니다."),
    
    // 422 Unprocessable Entity - 비즈니스 로직 오류
    EXCEEDS_MAXIMUM_POINT("EXCEEDS_MAXIMUM_POINT", HttpStatus.UNPROCESSABLE_ENTITY, "충전 후 포인트가 300만을 초과할 수 없습니다."),
    NOT_ENOUGH_POINT("NOT_ENOUGH_POINT", HttpStatus.UNPROCESSABLE_ENTITY, "포인트가 부족합니다."),
    OUT_OF_STOCK_PRODUCT("OUT_OF_STOCK_PRODUCT", HttpStatus.UNPROCESSABLE_ENTITY, "상품 재고가 부족합니다."),
    OUT_OF_STOCK_COUPON("OUT_OF_STOCK_COUPON", HttpStatus.UNPROCESSABLE_ENTITY, "쿠폰 재고가 부족합니다."),
    EXPIRED_COUPON("EXPIRED_COUPON", HttpStatus.UNPROCESSABLE_ENTITY, "쿠폰이 만료되었습니다."),
    
    // 500 Internal Server Error - 서버 내부 오류
    PAYMENT_PROCESSING_FAILED("PAYMENT_PROCESSING_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "결제 처리 중 예기치 못한 오류가 발생했습니다."),
    PAYMENT_FAILED("PAYMENT_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "결제에 실패했습니다."),

    // 기타 예상치 못한 오류
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    UNEXPECTED_ERROR("UNEXPECTED_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 발생했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;
} 