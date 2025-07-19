package kr.hhplus.be.server.infrastructure.persistence.coupon;

import kr.hhplus.be.server.domain.coupon.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {
    List<UserCoupon> findByUserIdAndIsUsedFalse(Long userId);
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
}
