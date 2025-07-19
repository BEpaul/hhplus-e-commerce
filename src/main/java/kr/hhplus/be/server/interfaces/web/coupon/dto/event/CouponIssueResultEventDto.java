package kr.hhplus.be.server.interfaces.web.coupon.dto.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueResultEventDto {
    
    private String requestId;
    private Long userId;
    private Long couponId;
    private boolean success;
    private String errorMessage;
    private Long userCouponId; // 발급 성공 시에만 설정
    private LocalDateTime processedTime;
    
    public static CouponIssueResultEventDto success(String requestId, Long userId, Long couponId, Long userCouponId) {
        return CouponIssueResultEventDto.builder()
                .requestId(requestId)
                .userId(userId)
                .couponId(couponId)
                .success(true)
                .userCouponId(userCouponId)
                .processedTime(LocalDateTime.now())
                .build();
    }
    
    public static CouponIssueResultEventDto failure(String requestId, Long userId, Long couponId, String errorMessage) {
        return CouponIssueResultEventDto.builder()
                .requestId(requestId)
                .userId(userId)
                .couponId(couponId)
                .success(false)
                .errorMessage(errorMessage)
                .processedTime(LocalDateTime.now())
                .build();
    }
} 