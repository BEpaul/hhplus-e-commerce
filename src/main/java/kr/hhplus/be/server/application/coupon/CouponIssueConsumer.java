package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import kr.hhplus.be.server.infrastructure.config.kafka.KafkaTopicConstants;
import kr.hhplus.be.server.interfaces.web.coupon.dto.event.CouponIssueRequestEventDto;
import kr.hhplus.be.server.interfaces.web.coupon.dto.event.CouponIssueResultEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static kr.hhplus.be.server.common.exception.ErrorCode.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponKafkaEventService couponKafkaEventService;

    /**
     * 쿠폰 발급 요청을 처리하는 Kafka Consumer
     * 같은 쿠폰 ID의 요청은 같은 파티션에서 순차 처리됨
     * 1. 쿠폰 존재 여부 확인
     * 2. 중복 발급 확인
     * 3. 재고 확인 및 차감
     * 4. UserCoupon 저장
     * 5. 성공 결과 이벤트 발행
     * 6. 실패 결과 이벤트 발행
     */
    @KafkaListener(
            topics = KafkaTopicConstants.COUPON_ISSUE_REQUEST,
            groupId = "coupon-issue-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleCouponIssueRequest(
            @Payload CouponIssueRequestEventDto requestEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {
        
        log.info("쿠폰 발급 요청 처리 시작 - 파티션: {}, 오프셋: {}, 요청 ID: {}, 사용자 ID: {}, 쿠폰 ID: {}", 
            partition, offset, requestEvent.getRequestId(), requestEvent.getUserId(), requestEvent.getCouponId());
        
        try {
            Coupon coupon = findCouponById(requestEvent.getCouponId());
            
            if (isAlreadyIssued(requestEvent.getUserId(), requestEvent.getCouponId())) {
                log.warn("중복 쿠폰 발급 시도 - 사용자 ID: {}, 쿠폰 ID: {}", 
                    requestEvent.getUserId(), requestEvent.getCouponId());
                
                CouponIssueResultEventDto resultEvent = CouponIssueResultEventDto.failure(
                    requestEvent.getRequestId(), 
                    requestEvent.getUserId(), 
                    requestEvent.getCouponId(), 
                    "이미 발급받은 쿠폰입니다."
                );
                
                couponKafkaEventService.publishCouponIssueResult(resultEvent);
                ack.acknowledge();
                return;
            }
            
            if (coupon.getStock() <= 0) {
                log.warn("쿠폰 재고 부족 - 쿠폰 ID: {}, 현재 재고: {}", 
                    requestEvent.getCouponId(), coupon.getStock());
                
                CouponIssueResultEventDto resultEvent = CouponIssueResultEventDto.failure(
                    requestEvent.getRequestId(), 
                    requestEvent.getUserId(), 
                    requestEvent.getCouponId(), 
                    "쿠폰 재고가 부족합니다."
                );
                
                couponKafkaEventService.publishCouponIssueResult(resultEvent);
                ack.acknowledge();
                return;
            }
            
            coupon.decreaseStock();
            couponRepository.save(coupon);
            
            UserCoupon userCoupon = UserCoupon.of(requestEvent.getUserId(), requestEvent.getCouponId());
            UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);
            
            CouponIssueResultEventDto resultEvent = CouponIssueResultEventDto.success(
                requestEvent.getRequestId(),
                requestEvent.getUserId(),
                requestEvent.getCouponId(),
                savedUserCoupon.getId()
            );
            
            couponKafkaEventService.publishCouponIssueResult(resultEvent);
            
            log.info("쿠폰 발급 처리 완료 - 요청 ID: {}, 사용자 ID: {}, 쿠폰 ID: {}, UserCoupon ID: {}, 남은 재고: {}", 
                requestEvent.getRequestId(), requestEvent.getUserId(), requestEvent.getCouponId(), 
                savedUserCoupon.getId(), coupon.getStock());
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("쿠폰 발급 처리 중 오류 발생 - 요청 ID: {}, 사용자 ID: {}, 쿠폰 ID: {}", 
                requestEvent.getRequestId(), requestEvent.getUserId(), requestEvent.getCouponId(), e);
            
            CouponIssueResultEventDto resultEvent = CouponIssueResultEventDto.failure(
                requestEvent.getRequestId(),
                requestEvent.getUserId(),
                requestEvent.getCouponId(),
                "쿠폰 발급 처리 중 오류가 발생했습니다: " + e.getMessage()
            );
            
            couponKafkaEventService.publishCouponIssueResult(resultEvent);
            ack.acknowledge();
        }
    }

    private Coupon findCouponById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new ApiException(COUPON_NOT_FOUND));
    }

    private boolean isAlreadyIssued(Long userId, Long couponId) {
        return userCouponRepository.existsByUserIdAndCouponId(userId, couponId);
    }
} 