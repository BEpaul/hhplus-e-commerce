package kr.hhplus.be.server.infrastructure.kafka.coupon;

import kr.hhplus.be.server.infrastructure.config.kafka.KafkaTopicConstants;
import kr.hhplus.be.server.interfaces.web.coupon.dto.event.CouponIssueRequestEventDto;
import kr.hhplus.be.server.interfaces.web.coupon.dto.event.CouponIssueResultEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponKafkaEventService {

    private final KafkaTemplate<String, CouponIssueRequestEventDto> couponIssueRequestKafkaTemplate;
    private final KafkaTemplate<String, CouponIssueResultEventDto> couponIssueResultKafkaTemplate;

    /**
     * 쿠폰 발급 요청 이벤트를 Kafka로 발행
     */
    public CompletableFuture<SendResult<String, CouponIssueRequestEventDto>> publishCouponIssueRequest(
            Long userId, Long couponId) {
        
        CouponIssueRequestEventDto event = CouponIssueRequestEventDto.of(userId, couponId);
        String key = couponId.toString(); // 쿠폰 ID를 키로 사용하여 같은 쿠폰의 요청은 같은 파티션으로 전송
        
        log.info("쿠폰 발급 요청 이벤트 발행 - 사용자 ID: {}, 쿠폰 ID: {}, 요청 ID: {}", 
            userId, couponId, event.getRequestId());
        
        return couponIssueRequestKafkaTemplate.send(KafkaTopicConstants.COUPON_ISSUE_REQUEST, key, event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("쿠폰 발급 요청 이벤트 발행 실패 - 사용자 ID: {}, 쿠폰 ID: {}", 
                            userId, couponId, throwable);
                    } else {
                        log.info("쿠폰 발급 요청 이벤트 발행 성공 - 사용자 ID: {}, 쿠폰 ID: {}, 파티션: {}, 오프셋: {}", 
                            userId, couponId, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * 쿠폰 발급 결과 이벤트를 Kafka로 발행
     */
    public void publishCouponIssueResult(
            CouponIssueResultEventDto resultEvent) {
        
        String key = resultEvent.getCouponId().toString();
        
        log.info("쿠폰 발급 결과 이벤트 발행 - 요청 ID: {}, 성공: {}", 
            resultEvent.getRequestId(), resultEvent.isSuccess());

        couponIssueResultKafkaTemplate.send(KafkaTopicConstants.COUPON_ISSUE_RESULT, key, resultEvent)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("쿠폰 발급 결과 이벤트 발행 실패 - 요청 ID: {}",
                                resultEvent.getRequestId(), throwable);
                    } else {
                        log.info("쿠폰 발급 결과 이벤트 발행 성공 - 요청 ID: {}, 파티션: {}, 오프셋: {}",
                                resultEvent.getRequestId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }
} 