package kr.hhplus.be.server.interfaces.web.coupon.dto.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueRequestEventDto {
    
    private Long userId;
    private Long couponId;
    private LocalDateTime requestTime;
    private String requestId; // 중복 요청 방지를 위한 고유 ID
    
    public static CouponIssueRequestEventDto of(Long userId, Long couponId) {
        return CouponIssueRequestEventDto.builder()
                .userId(userId)
                .couponId(couponId)
                .requestTime(LocalDateTime.now())
                .requestId(generateRequestId(userId, couponId))
                .build();
    }
    
    private static String generateRequestId(Long userId, Long couponId) {
        return userId + "_" + couponId + "_" + System.currentTimeMillis();
    }
} 