package kr.hhplus.be.server.infrastructure.external.coupon;

public enum CouponOutBoxEventStatus {
    PENDING,    // 대기 중
    COMPLETED,  // 처리 완료
    FAILED      // 처리 실패
}
