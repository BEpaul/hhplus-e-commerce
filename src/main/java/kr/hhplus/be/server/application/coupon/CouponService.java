package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.common.exception.NotFoundCouponException;
import kr.hhplus.be.server.common.exception.NotOwnedUserCouponException;
import kr.hhplus.be.server.common.exception.OutOfStockCouponException;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional
    public void useCoupon(Long userCouponId) {
        UserCoupon userCoupon = findUserCouponById(userCouponId);
        userCoupon.use();
        userCouponRepository.save(userCoupon);
    }

    @Transactional
    public Long calculateDiscountPrice(Long userCouponId, Long totalPrice) {
        UserCoupon userCoupon = findUserCouponById(userCouponId);
        userCoupon.isExpired();

        Coupon coupon = findCouponById(userCoupon.getCouponId());
        return coupon.apply(totalPrice);
    }
    
    @Transactional
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        Coupon coupon = findCouponById(couponId);

        if (coupon.getStock() <= 0) {
            throw new OutOfStockCouponException("쿠폰 재고가 부족합니다.");
        }

        try {
            coupon.decreaseStock();
            couponRepository.save(coupon);
            
            UserCoupon userCoupon = UserCoupon.of(userId, couponId);
            return userCouponRepository.save(userCoupon);
        } catch (OptimisticLockingFailureException e) {
            throw new OutOfStockCouponException("다른 사용자가 먼저 쿠폰을 발급받았습니다.");
        }
    }

    private UserCoupon findUserCouponById(Long userCouponId) {
        return userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new NotOwnedUserCouponException("사용자가 갖고 있지 않은 쿠폰입니다."));
    }

    private Coupon findCouponById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new NotFoundCouponException("쿠폰이 존재하지 않습니다."));
    }
}
