package kr.hhplus.be.server.domain.coupon;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.ApiException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static kr.hhplus.be.server.common.exception.ErrorCode.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_coupon_id")
    private Long id;
    private Long userId;
    private Long couponId;
    private boolean isUsed;
    private LocalDateTime expiredAt;

    @Builder
    public UserCoupon(Long id, Long userId, Long couponId, boolean isUsed, LocalDateTime expiredAt) {
        this.id = id;
        this.userId = userId;
        this.couponId = couponId;
        this.isUsed = isUsed;
        this.expiredAt = expiredAt;
    }

    public void use() {
        if (this.isUsed) {
            throw new ApiException(ALREADY_USED_COUPON);
        }

        this.isUsed = true;
    }

    public void isExpired() {
        if (LocalDateTime.now().isAfter(this.expiredAt)) {
            throw new ApiException(EXPIRED_COUPON);
        }
    }

    public static UserCoupon of(Long userId, Long couponId) {
        return UserCoupon.builder()
                .userId(userId)
                .couponId(couponId)
                .isUsed(false)
                .expiredAt(LocalDateTime.now().plusDays(30)) // 기본 만료 기간을 30일로 설정
                .build();
    }
}
