package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.infrastructure.kafka.coupon.CouponKafkaEventService;
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
    private final CouponKafkaEventService couponKafkaEventService;

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
     * 쿠폰 발급 (Kafka 기반 비동기 처리)
     * 1. 쿠폰 존재 여부 확인
     * 2. 재고 검사
     * 3. 중복 발급 확인
     * 4. Kafka로 쿠폰 발급 요청 이벤트 발행
     */
    public void issueCoupon(Long userId, Long couponId) {
        log.info("쿠폰 발급 요청 시작 - 사용자 ID: {}, 쿠폰 ID: {}", userId, couponId);
        
        Coupon coupon = findCouponById(couponId);
        
        if (coupon.getStock() <= 0) {
            log.warn("쿠폰 재고 부족 - 쿠폰 ID: {}, 현재 재고: {}", couponId, coupon.getStock());
            throw new ApiException(COUPON_ISSUANCE_FAILED);
        }
        
        if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            log.warn("중복 쿠폰 발급 시도 - 사용자 ID: {}, 쿠폰 ID: {}", userId, couponId);
            throw new ApiException(COUPON_ALREADY_ISSUED);
        }
        
        couponKafkaEventService.publishCouponIssueRequest(userId, couponId);
        
        log.info("쿠폰 발급 요청 이벤트 발행 완료 - 사용자 ID: {}, 쿠폰 ID: {}", userId, couponId);
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
