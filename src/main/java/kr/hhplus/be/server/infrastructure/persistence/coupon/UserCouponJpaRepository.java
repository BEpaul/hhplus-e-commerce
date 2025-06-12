package kr.hhplus.be.server.infrastructure.persistence.coupon;

import kr.hhplus.be.server.domain.coupon.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {
}
