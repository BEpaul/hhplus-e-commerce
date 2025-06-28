package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.common.exception.*;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import kr.hhplus.be.server.infrastructure.config.redis.DistributedLockService;
import kr.hhplus.be.server.interfaces.web.coupon.dto.response.CouponListResponse;
import kr.hhplus.be.server.interfaces.web.coupon.dto.response.CouponResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static kr.hhplus.be.server.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final DistributedLockService distributedLockService;

    /**
     * 쿠폰 사용
     */
    @Transactional
    public void useCoupon(Long userCouponId) {
        UserCoupon userCoupon = findUserCouponById(userCouponId);
        
        // 사용자별 분산락 적용
        distributedLockService.executePointLock(userCoupon.getUserId(), () -> {
            log.info("쿠폰 사용 시작 - 사용자 쿠폰 ID: {}, 사용자 ID: {}", userCouponId, userCoupon.getUserId());
            
            userCoupon.use();
            userCouponRepository.save(userCoupon);
            
            log.info("쿠폰 사용 완료 - 사용자 쿠폰 ID: {}", userCouponId);
            return null;
        });
    }

    @Transactional
    public Long calculateDiscountPrice(Long userCouponId, Long totalPrice) {
        UserCoupon userCoupon = findUserCouponById(userCouponId);
        userCoupon.isExpired();

        Coupon coupon = findCouponById(userCoupon.getCouponId());
        return coupon.apply(totalPrice);
    }

    /**
     * 쿠폰 발급
     */
    @Transactional
    @Retryable(
            value = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 10)
    )
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        // 쿠폰별 분산락 적용
        return distributedLockService.executeWithLock("coupon:issue:" + couponId, 10, 30, () -> {
            log.info("쿠폰 발급 시작 - 사용자 ID: {}, 쿠폰 ID: {}", userId, couponId);
            
            Coupon coupon = findCouponById(couponId);

            if (coupon.getStock() <= 0) {
                log.warn("쿠폰 재고 부족 - 쿠폰 ID: {}", couponId);
                throw new ApiException(OUT_OF_STOCK_COUPON);
            }

            coupon.decreaseStock();
            couponRepository.save(coupon);
            
            UserCoupon userCoupon = UserCoupon.of(userId, couponId);
            UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);
            
            log.info("쿠폰 발급 완료 - 사용자 ID: {}, 쿠폰 ID: {}, 남은 재고: {}", userId, couponId, coupon.getStock());
            return savedUserCoupon;
        });
    }

    /**
     * 사용자가 보유한 쿠폰 목록을 조회
     */
    @Transactional(readOnly = true)
    public CouponListResponse getUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findUnusedByUserId(userId);

        if (userCoupons.isEmpty()) {
            return CouponListResponse.of(List.of());
        }

        List<CouponResponse> couponResponses = userCoupons.stream()
                .map(userCoupon -> {
                    Coupon coupon = findCouponById(userCoupon.getCouponId());
                    return CouponResponse.of(userCoupon, coupon);
                })
                .collect(Collectors.toList());

        return CouponListResponse.of(couponResponses);
    }

    private UserCoupon findUserCouponById(Long userCouponId) {
        return userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new ApiException(NOT_OWNED_USER_COUPON));
    }

    private Coupon findCouponById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new ApiException(COUPON_NOT_FOUND));
    }
}
