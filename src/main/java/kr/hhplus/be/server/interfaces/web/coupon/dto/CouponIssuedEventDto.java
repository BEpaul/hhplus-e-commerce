package kr.hhplus.be.server.interfaces.web.coupon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssuedEventDto {
    private Long couponId;
    private Long userId;
    private Long userCouponId;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issuedAt;
    
    public static CouponIssuedEventDto of(Long couponId, Long userId, Long userCouponId) {
        return CouponIssuedEventDto.builder()
                .couponId(couponId)
                .userId(userId)
                .userCouponId(userCouponId)
                .issuedAt(LocalDateTime.now())
                .build();
    }
} 