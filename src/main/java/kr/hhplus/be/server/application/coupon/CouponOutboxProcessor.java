package kr.hhplus.be.server.application.coupon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.interfaces.web.coupon.dto.event.CouponIssuedEventDto;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.coupon.event.CouponOutBoxEvent;
import kr.hhplus.be.server.domain.coupon.event.CouponOutBoxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponOutboxProcessor {

    private final CouponOutBoxEventRepository outBoxEventRepository;
    private final CouponRepository couponRepository;
    private final ObjectMapper objectMapper;

    private static final String COUPON_ISSUED_EVENT_TYPE = "COUPON_ISSUED";

    /**
     * 쿠폰 발급 이벤트 비동기 처리 (RDB 재고 동기화)
     */
    @Scheduled(fixedDelay = 10000) // 10초마다 실행
    public void processCouponIssuedEvents() {
        log.debug("쿠폰 발급 이벤트 비동기 처리 시작");
        
        try {
            List<CouponOutBoxEvent> pendingEvents = outBoxEventRepository.findPendingEventsByType(COUPON_ISSUED_EVENT_TYPE, 100);
            
            for (CouponOutBoxEvent event : pendingEvents) {
                processEventWithTransaction(event);
            }
            
        } catch (Exception e) {
            log.error("쿠폰 발급 이벤트 처리 중 오류 발생", e);
        }
    }

    /**
     * 실패한 이벤트 재처리 스케줄러
     */
    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    public void retryFailedEvents() {
        log.debug("실패한 쿠폰 발급 이벤트 재처리 시작");
        
        try {
            List<CouponOutBoxEvent> failedEvents = outBoxEventRepository.findFailedEventsForRetry();
            
            for (CouponOutBoxEvent event : failedEvents) {
                processEventWithTransaction(event);
            }
            
        } catch (Exception e) {
            log.error("실패한 쿠폰 발급 이벤트 재처리 중 오류 발생", e);
        }
    }

    /**
     * 개별 이벤트를 트랜잭션으로 처리
     */
    @Transactional
    public void processEventWithTransaction(CouponOutBoxEvent event) {
        try {
            processCouponIssuedEvent(event);
            event.markAsProcessed();
            outBoxEventRepository.save(event);
            
            log.debug("쿠폰 발급 이벤트 처리 완료 - 이벤트 ID: {}", event.getId());
            
        } catch (Exception e) {
            log.error("쿠폰 발급 이벤트 처리 실패 - 이벤트 ID: {}", event.getId(), e);
            
            if (event.canRetry()) {
                event.markAsFailed();
                outBoxEventRepository.save(event);
            } else {
                log.error("쿠폰 발급 이벤트 최대 재시도 횟수 초과 - 이벤트 ID: {}", event.getId());
                // TODO: 알림 발송 또는 수동 처리 필요
            }
        }
    }

    /**
     * 쿠폰 발급 이벤트 처리 (RDB 쿠폰 재고 차감)
     */
    private void processCouponIssuedEvent(CouponOutBoxEvent event) throws JsonProcessingException {
        CouponIssuedEventDto couponEvent = objectMapper.readValue(event.getPayload(), CouponIssuedEventDto.class);
        
        // RDB 쿠폰 재고 차감
        Coupon coupon = couponRepository.findById(couponEvent.getCouponId())
                .orElseThrow(() -> new RuntimeException("쿠폰을 찾을 수 없습니다: " + couponEvent.getCouponId()));
        
        coupon.decreaseStock();
        couponRepository.save(coupon);
        
        log.info("RDB 쿠폰 재고 차감 완료 - 쿠폰 ID: {}, 남은 재고: {}", 
            couponEvent.getCouponId(), coupon.getStock());
    }
} 