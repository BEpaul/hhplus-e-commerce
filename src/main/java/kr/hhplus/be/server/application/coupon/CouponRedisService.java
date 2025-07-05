package kr.hhplus.be.server.application.coupon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String COUPON_QUEUE_KEY_PREFIX = "coupon:queue:";
    private static final String COUPON_LIMIT_KEY_PREFIX = "coupon:limit:";
    private static final String COUPON_ISSUED_KEY_PREFIX = "coupon:issued:";
    private static final Duration COUPON_DATA_TTL = Duration.ofDays(31); // TTL: 31일

    /**
     * 쿠폰 발급 시도
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @return 발급 성공 여부
     */
    public boolean tryIssueCoupon(Long couponId, Long userId) {
        String queueKey = getQueueKey(couponId);
        String issuedKey = getIssuedKey(couponId, userId);

        try {
            // 1. 사용자별 발급 상태 확인
            if (isAlreadyIssued(couponId, userId)) {
                log.warn("이미 발급된 쿠폰 - 쿠폰 ID: {}, 사용자 ID: {}", couponId, userId);
                return false;
            }

            // 2. 쿠폰 발급 대기열에 추가 (timestamp를 score로 사용)
            Long timestamp = System.currentTimeMillis();
            redisTemplate.opsForZSet().add(queueKey, userId.toString(), timestamp);

            // 3. 발급 순위 확인
            Long userRank = redisTemplate.opsForZSet().rank(queueKey, userId.toString());
            if (userRank == null) {
                log.error("발급 순위 확인 실패 - 쿠폰 ID: {}, 사용자 ID: {}", couponId, userId);
                return false;
            }

            // 4. 쿠폰 수량 제한 확인
            Long couponLimit = getCouponLimit(couponId);
            if (userRank >= couponLimit) {
                redisTemplate.opsForZSet().remove(queueKey, userId.toString());
                log.warn("쿠폰 수량 초과 - 쿠폰 ID: {}, 사용자 ID: {}, 순위: {}, 제한: {}", 
                    couponId, userId, userRank, couponLimit);
                return false;
            }

            // 5. 발급 완료 처리
            redisTemplate.opsForValue().set(issuedKey, "1", COUPON_DATA_TTL);
            log.info("쿠폰 발급 성공 - 쿠폰 ID: {}, 사용자 ID: {}, 순위: {}", couponId, userId, userRank);
            return true;

        } catch (Exception e) {
            log.error("쿠폰 발급 처리 중 오류 발생 - 쿠폰 ID: {}, 사용자 ID: {}", couponId, userId, e);
            redisTemplate.opsForZSet().remove(queueKey, userId.toString());
            return false;
        }
    }

    /**
     * 사용자별 쿠폰 발급 상태 확인
     */
    public boolean isAlreadyIssued(Long couponId, Long userId) {
        String issuedKey = getIssuedKey(couponId, userId);
        return redisTemplate.hasKey(issuedKey);
    }

    /**
     * 쿠폰 발급 순위 조회
     */
    public Long getIssueRank(Long couponId, Long userId) {
        String queueKey = getQueueKey(couponId);
        return redisTemplate.opsForZSet().rank(queueKey, userId.toString());
    }

    /**
     * 쿠폰 발급 완료 수 조회
     */
    public Long getIssuedCount(Long couponId) {
        String queueKey = getQueueKey(couponId);
        return redisTemplate.opsForZSet().size(queueKey);
    }

    /**
     * 쿠폰 수량 제한 설정
     */
    public void setCouponLimit(Long couponId, Long limit) {
        String limitKey = getLimitKey(couponId);
        redisTemplate.opsForValue().set(limitKey, limit.toString(), COUPON_DATA_TTL);
        log.info("쿠폰 수량 제한 설정 - 쿠폰 ID: {}, 제한: {}", couponId, limit);
    }

    /**
     * 쿠폰 수량 제한 조회
     */
    public Long getCouponLimit(Long couponId) {
        String limitKey = getLimitKey(couponId);
        String limitStr = redisTemplate.opsForValue().get(limitKey);
        return limitStr != null ? Long.parseLong(limitStr) : 0L;
    }

    /**
     * 쿠폰 발급 대기열 초기화 (자정 스케줄러용)
     */
    public void initializeCouponQueue(Long couponId, Set<String> userIds) {
        String queueKey = getQueueKey(couponId);
        
        // 기존 데이터 삭제
        redisTemplate.delete(queueKey);
        
        // 모든 사용자를 0점으로 초기 등록
        if (!userIds.isEmpty()) {
            for (String userId : userIds) {
                redisTemplate.opsForZSet().add(queueKey, userId, 0.0);
            }
        }
        
        redisTemplate.expire(queueKey, COUPON_DATA_TTL);
        
        log.info("쿠폰 발급 대기열 초기화 완료 - 쿠폰 ID: {}, 사용자 수: {}", couponId, userIds.size());
    }

    /**
     * 쿠폰 발급 데이터 삭제 (정리용)
     */
    public void deleteCouponData(Long couponId) {
        String queueKey = getQueueKey(couponId);
        String limitKey = getLimitKey(couponId);
        
        redisTemplate.delete(queueKey);
        redisTemplate.delete(limitKey);
        
        log.info("쿠폰 발급 데이터 삭제 완료 - 쿠폰 ID: {}", couponId);
    }

    private String getQueueKey(Long couponId) {
        return COUPON_QUEUE_KEY_PREFIX + couponId;
    }

    private String getLimitKey(Long couponId) {
        return COUPON_LIMIT_KEY_PREFIX + couponId;
    }

    private String getIssuedKey(Long couponId, Long userId) {
        return COUPON_ISSUED_KEY_PREFIX + couponId + ":" + userId;
    }
} 