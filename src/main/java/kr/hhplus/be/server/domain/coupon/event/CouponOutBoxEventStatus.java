package kr.hhplus.be.server.domain.coupon.event;

public enum CouponOutBoxEventStatus {
    PENDING,    // 대기 중
    PROCESSED,  // 처리 완료
    FAILED      // 처리 실패
}
