package kr.hhplus.be.server.domain.order;

import kr.hhplus.be.server.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static kr.hhplus.be.server.common.exception.ErrorCode.ALREADY_APPLIED_COUPON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class OrderTest {

    @Test
    void 주문_엔티티를_성공적으로_생성한다() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        Long totalAmount = 10000L;
        boolean isCouponApplied = false;

        // when
        Order order = Order.builder()
                .userId(userId)
                .userCouponId(userCouponId)
                .totalAmount(totalAmount)
                .isCouponApplied(isCouponApplied)
                .build();

        // then
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getUserCouponId()).isEqualTo(userCouponId);
        assertThat(order.getTotalAmount()).isEqualTo(totalAmount);
        assertThat(order.isCouponApplied()).isEqualTo(isCouponApplied);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.WAITING);
    }

    @Test
    void 쿠폰이_적용되지_않은_주문에_쿠폰을_적용한다() {
        // given
        Order order = Order.builder()
                .userId(1L)
                .totalAmount(10000L)
                .isCouponApplied(false)
                .build();

        // when
        order.applyCoupon();

        // then
        assertThat(order.isCouponApplied()).isTrue();
    }

    @Test
    void 이미_쿠폰이_적용된_주문에_쿠폰을_다시_적용하면_예외가_발생한다() {
        // given
        Order order = Order.builder()
                .userId(1L)
                .totalAmount(10000L)
                .isCouponApplied(true)
                .build();

        // when & then
        assertThatThrownBy(() -> order.applyCoupon())
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ALREADY_APPLIED_COUPON);
    }

    @Test
    void 주문의_총_금액을_계산한다() {
        // given
        Order order = Order.builder()
                .userId(1L)
                .totalAmount(10000L)
                .build();
        Long newTotalAmount = 15000L;

        // when
        order.calculateTotalAmount(newTotalAmount);

        // then
        assertThat(order.getTotalAmount()).isEqualTo(newTotalAmount);
    }

    @Test
    void 주문을_성공_상태로_변경한다() {
        // given
        Order order = Order.builder()
                .userId(1L)
                .totalAmount(10000L)
                .build();

        // when
        order.success();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void 주문을_실패_상태로_변경한다() {
        // given
        Order order = Order.builder()
                .userId(1L)
                .totalAmount(10000L)
                .build();

        // when
        order.markAsFailed();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    void 주문_생성_시_기본_상태는_WAITING이다() {
        // given & when
        Order order = Order.builder()
                .userId(1L)
                .totalAmount(10000L)
                .build();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.WAITING);
    }

    @Test
    void 주문_생성_시_ID가_null이면_자동_생성된다() {
        // given & when
        Order order = Order.builder()
                .userId(1L)
                .totalAmount(10000L)
                .build();

        // then
        assertThat(order.getId()).isNull();
    }

    @Test
    void 주문_생성_시_명시적으로_ID를_설정할_수_있다() {
        // given
        Long orderId = 1L;

        // when
        Order order = Order.builder()
                .id(orderId)
                .userId(1L)
                .totalAmount(10000L)
                .build();

        // then
        assertThat(order.getId()).isEqualTo(orderId);
    }
}
