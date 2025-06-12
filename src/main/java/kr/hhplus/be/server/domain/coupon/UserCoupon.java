package kr.hhplus.be.server.domain.coupon;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.AlreadyUsedCouponException;
import kr.hhplus.be.server.common.exception.ExpiredCouponException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
            throw new AlreadyUsedCouponException("이미 사용된 쿠폰입니다.");
        }

        this.isUsed = true;
    }

    public void isExpired() {
        if (LocalDateTime.now().isAfter(this.expiredAt)) {
            throw new ExpiredCouponException("쿠폰이 만료되었습니다.");
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
