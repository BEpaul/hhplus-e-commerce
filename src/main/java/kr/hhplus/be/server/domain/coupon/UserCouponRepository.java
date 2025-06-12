package kr.hhplus.be.server.domain.coupon;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {
    UserCoupon save(UserCoupon userCoupon);
    Optional<UserCoupon> findById(Long userCouponId);
    List<UserCoupon> findAll();
}
