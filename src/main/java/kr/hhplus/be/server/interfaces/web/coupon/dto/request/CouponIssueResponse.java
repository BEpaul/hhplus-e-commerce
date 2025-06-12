package kr.hhplus.be.server.interfaces.web.coupon.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponIssueResponse {

    private Long couponId;

    public static CouponIssueResponse from(Long couponId) {
        return CouponIssueResponse.builder()
                .couponId(couponId)
                .build();
    }
}
