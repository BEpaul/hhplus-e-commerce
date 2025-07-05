package kr.hhplus.be.server.interfaces.web.coupon.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponIssueStatusResponse {
    private Long couponId;
    private Long userId;
    private Long issueRank;      // 발급 순위
    private Long issuedCount;    // 발급 완료 수
    private Long totalLimit;     // 총 발급 제한 수
    private boolean isIssued;    // 발급 성공 여부
} 