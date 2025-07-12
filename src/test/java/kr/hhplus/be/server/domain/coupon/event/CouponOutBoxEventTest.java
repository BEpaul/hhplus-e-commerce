package kr.hhplus.be.server.domain.coupon.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CouponOutBoxEventTest {

    @Test
    void 쿠폰_아웃박스_이벤트_엔티티를_성공적으로_생성한다() {
        // given
        String eventType = "COUPON_ISSUED";
        String payload = "{\"userId\":1,\"couponId\":1}";
        CouponOutBoxEventStatus status = CouponOutBoxEventStatus.PENDING;
        Long retryCount = 0L;

        // when
        CouponOutBoxEvent event = CouponOutBoxEvent.builder()
                .eventType(eventType)
                .payload(payload)
                .status(status)
                .retryCount(retryCount)
                .build();

        // then
        assertThat(event.getEventType()).isEqualTo(eventType);
        assertThat(event.getPayload()).isEqualTo(payload);
        assertThat(event.getStatus()).isEqualTo(status);
        assertThat(event.getRetryCount()).isEqualTo(retryCount);
    }

    @Test
    void 쿠폰_아웃박스_이벤트_생성_시_기본값이_설정된다() {
        // given
        String eventType = "COUPON_ISSUED";
        String payload = "{\"userId\":1,\"couponId\":1}";

        // when
        CouponOutBoxEvent event = CouponOutBoxEvent.builder()
                .eventType(eventType)
                .payload(payload)
                .build();

        // then
        assertThat(event.getStatus()).isEqualTo(CouponOutBoxEventStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(0L);
    }

    @Test
    void 쿠폰_아웃박스_이벤트를_처리_완료_상태로_변경한다() {
        // given
        CouponOutBoxEvent event = CouponOutBoxEvent.builder()
                .eventType("COUPON_ISSUED")
                .payload("{\"userId\":1,\"couponId\":1}")
                .status(CouponOutBoxEventStatus.PENDING)
                .retryCount(0L)
                .build();

        // when
        event.markAsProcessed();

        // then
        assertThat(event.getStatus()).isEqualTo(CouponOutBoxEventStatus.PROCESSED);
    }

    @Test
    void 쿠폰_아웃박스_이벤트를_실패_상태로_변경한다() {
        // given
        CouponOutBoxEvent event = CouponOutBoxEvent.builder()
                .eventType("COUPON_ISSUED")
                .payload("{\"userId\":1,\"couponId\":1}")
                .status(CouponOutBoxEventStatus.PENDING)
                .retryCount(0L)
                .build();

        // when
        event.markAsFailed();

        // then
        assertThat(event.getStatus()).isEqualTo(CouponOutBoxEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1L);
    }

    @Test
    void 쿠폰_아웃박스_이벤트_재시도_횟수가_증가한다() {
        // given
        CouponOutBoxEvent event = CouponOutBoxEvent.builder()
                .eventType("COUPON_ISSUED")
                .payload("{\"userId\":1,\"couponId\":1}")
                .status(CouponOutBoxEventStatus.PENDING)
                .retryCount(1L)
                .build();

        // when
        event.markAsFailed();

        // then
        assertThat(event.getRetryCount()).isEqualTo(2L);
    }

    @Test
    void 쿠폰_아웃박스_이벤트_재시도_가능_여부를_확인한다() {
        // given
        CouponOutBoxEvent event = CouponOutBoxEvent.builder()
                .eventType("COUPON_ISSUED")
                .payload("{\"userId\":1,\"couponId\":1}")
                .status(CouponOutBoxEventStatus.FAILED)
                .retryCount(2L)
                .build();

        // when & then
        assertThat(event.canRetry()).isTrue(); // 2 < 3
    }

    @Test
    void 쿠폰_아웃박스_이벤트_재시도_불가능_여부를_확인한다() {
        // given
        CouponOutBoxEvent event = CouponOutBoxEvent.builder()
                .eventType("COUPON_ISSUED")
                .payload("{\"userId\":1,\"couponId\":1}")
                .status(CouponOutBoxEventStatus.FAILED)
                .retryCount(3L)
                .build();

        // when & then
        assertThat(event.canRetry()).isFalse(); // 3 >= 3
    }

    @Test
    void 쿠폰_아웃박스_이벤트_생성_시_ID가_null이면_자동_생성된다() {
        // given & when
        CouponOutBoxEvent event = CouponOutBoxEvent.builder()
                .eventType("COUPON_ISSUED")
                .payload("{\"userId\":1,\"couponId\":1}")
                .build();

        // then
        assertThat(event.getId()).isNull(); // JPA가 저장 시점에 ID를 생성하므로 null
    }

    @Test
    void 쿠폰_아웃박스_이벤트_생성_시_명시적으로_ID를_설정할_수_있다() {
        // given
        Long eventId = 1L;

        // when
        CouponOutBoxEvent event = CouponOutBoxEvent.builder()
                .id(eventId)
                .eventType("COUPON_ISSUED")
                .payload("{\"userId\":1,\"couponId\":1}")
                .build();

        // then
        assertThat(event.getId()).isEqualTo(eventId);
    }
} 