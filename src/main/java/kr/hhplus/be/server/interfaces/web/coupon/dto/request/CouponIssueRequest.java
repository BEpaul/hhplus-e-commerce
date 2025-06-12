package kr.hhplus.be.server.interfaces.web.coupon.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CouponIssueRequest {

    private Long userId;
    private Long couponId;
}
