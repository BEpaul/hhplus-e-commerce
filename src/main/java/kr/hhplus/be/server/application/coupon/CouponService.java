package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.common.exception.*;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import kr.hhplus.be.server.interfaces.web.coupon.dto.response.CouponListResponse;
import kr.hhplus.be.server.interfaces.web.coupon.dto.response.CouponResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final CouponRedisService couponRedisService;
    private final CouponEventService couponEventService;

    /**
     * 쿠폰 사용
     */
    @Transactional
    public void useCoupon(Long userCouponId) {
        log.info("쿠폰 사용 시작 - 사용자 쿠폰 ID: {}", userCouponId);
        
        UserCoupon userCoupon = findUserCouponById(userCouponId);
        userCoupon.use();
        userCouponRepository.save(userCoupon);
        
        log.info("쿠폰 사용 완료 - 사용자 쿠폰 ID: {}", userCouponId);
    }

    @Transactional
    public Long calculateDiscountPrice(Long userCouponId, Long totalPrice) {
        UserCoupon userCoupon = findUserCouponById(userCouponId);
        userCoupon.isExpired();

        Coupon coupon = findCouponById(userCoupon.getCouponId());
        return coupon.apply(totalPrice);
    }

    /**
     * 쿠폰 발급 (Redis 기반 선착순 처리 + 비동기 RDB 동기화)
     */
    @Transactional
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        log.info("쿠폰 발급 시작 - 사용자 ID: {}, 쿠폰 ID: {}", userId, couponId);
        
        Coupon coupon = findCouponById(couponId);

        // 1. 쿠폰 수량 제한을 Redis에 설정 (최초 1회)
        if (couponRedisService.getCouponLimit(couponId) == 0L) {
            couponRedisService.setCouponLimit(couponId, coupon.getStock());
        }

        // 2. Redis를 통한 선착순 쿠폰 발급 시도
        boolean isIssued = couponRedisService.tryIssueCoupon(couponId, userId);
        
        if (!isIssued) {
            log.warn("쿠폰 발급 실패 - 사용자 ID: {}, 쿠폰 ID: {}", userId, couponId);
            throw new ApiException(COUPON_ISSUANCE_FAILED);
        }

        // 3. Redis 발급 성공 시 RDB에 UserCoupon 저장
        UserCoupon userCoupon = UserCoupon.of(userId, couponId);
        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);
        
        // 4. 쿠폰 발급 완료 이벤트 발행 (비동기 RDB 동기화용)
        couponEventService.publishCouponIssuedEvent(couponId, userId, savedUserCoupon.getId());
        
        log.info("쿠폰 발급 완료 - 사용자 ID: {}, 쿠폰 ID: {}, UserCoupon ID: {}", 
            userId, couponId, savedUserCoupon.getId());
        
        return savedUserCoupon;
    }

    /**
     * 쿠폰 발급 순위 조회
     */
    public Long getIssueRank(Long couponId, Long userId) {
        return couponRedisService.getIssueRank(couponId, userId);
    }

    /**
     * 쿠폰 발급 완료 수 조회
     */
    public Long getIssuedCount(Long couponId) {
        return couponRedisService.getIssuedCount(couponId);
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
