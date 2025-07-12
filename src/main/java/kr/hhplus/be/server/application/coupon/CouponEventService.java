package kr.hhplus.be.server.application.coupon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.interfaces.web.coupon.dto.event.CouponIssuedEventDto;
import kr.hhplus.be.server.domain.coupon.event.CouponOutBoxEvent;
import kr.hhplus.be.server.domain.coupon.event.CouponOutBoxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponEventService {

    private final CouponOutBoxEventRepository outBoxEventRepository;
    private final ObjectMapper objectMapper;

    private static final String COUPON_ISSUED_EVENT_TYPE = "COUPON_ISSUED";

    /**
     * 쿠폰 발급 완료 이벤트 발행
     */
    @Transactional
    public void publishCouponIssuedEvent(Long couponId, Long userId, Long userCouponId) {
        try {
            CouponIssuedEventDto event = CouponIssuedEventDto.of(couponId, userId, userCouponId);
            String payload = objectMapper.writeValueAsString(event);

            CouponOutBoxEvent outBoxEvent = CouponOutBoxEvent.builder()
                    .eventType(COUPON_ISSUED_EVENT_TYPE)
                    .payload(payload)
                    .build();

            outBoxEventRepository.save(outBoxEvent);
            log.info("쿠폰 발급 이벤트 발행 완료 - 쿠폰 ID: {}, 사용자 ID: {}, UserCoupon ID: {}", 
                couponId, userId, userCouponId);

        } catch (JsonProcessingException e) {
            log.error("쿠폰 발급 이벤트 직렬화 실패 - 쿠폰 ID: {}, 사용자 ID: {}", couponId, userId, e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }
} 