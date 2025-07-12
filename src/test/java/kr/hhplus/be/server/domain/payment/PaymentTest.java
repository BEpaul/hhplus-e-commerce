package kr.hhplus.be.server.domain.payment;

import kr.hhplus.be.server.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static kr.hhplus.be.server.common.exception.ErrorCode.PAYMENT_INFO_NOT_EXIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PaymentTest {

    @Test
    void 결제_엔티티를_성공적으로_생성한다() {
        // given
        Long orderId = 1L;
        String idempotencyKey = "ORDER_1_1234567890";
        PaymentMethod paymentMethod = PaymentMethod.POINT;
        Long amount = 10000L;

        // when
        Payment payment = Payment.builder()
                .orderId(orderId)
                .idempotencyKey(idempotencyKey)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.PENDING)
                .build();

        // then
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getPaymentMethod()).isEqualTo(paymentMethod);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void 정상적인_결제_정보로_결제를_생성한다() {
        // given
        Long orderId = 1L;
        String idempotencyKey = "ORDER_1_1234567890";
        PaymentMethod paymentMethod = PaymentMethod.POINT;
        Long amount = 10000L;

        // when
        Payment payment = Payment.create(orderId, idempotencyKey, paymentMethod, amount);

        // then
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getPaymentMethod()).isEqualTo(paymentMethod);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getApprovedAt()).isNull();
        assertThat(payment.getCanceledAt()).isNull();
    }

    @Test
    void 주문ID가_null이면_결제_생성_시_예외가_발생한다() {
        // given
        Long orderId = null;
        String idempotencyKey = "ORDER_null_1234567890";
        PaymentMethod paymentMethod = PaymentMethod.POINT;
        Long amount = 10000L;

        // when & then
        assertThatThrownBy(() -> Payment.create(orderId, idempotencyKey, paymentMethod, amount))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", PAYMENT_INFO_NOT_EXIST);
    }

    @Test
    void 결제를_승인_상태로_변경한다() {
        // given
        Payment payment = Payment.builder()
                .orderId(1L)
                .idempotencyKey("ORDER_1_1234567890")
                .amount(10000L)
                .paymentMethod(PaymentMethod.POINT)
                .status(PaymentStatus.PENDING)
                .build();

        // when
        payment.markAsApproved();

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getApprovedAt()).isNotNull();
        assertThat(payment.getCanceledAt()).isNull();
    }

    @Test
    void 결제를_취소_상태로_변경한다() {
        // given
        Payment payment = Payment.builder()
                .orderId(1L)
                .idempotencyKey("ORDER_1_1234567890")
                .amount(10000L)
                .paymentMethod(PaymentMethod.POINT)
                .status(PaymentStatus.PENDING)
                .build();

        // when
        payment.markAsCanceled();

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getCanceledAt()).isNotNull();
        assertThat(payment.getApprovedAt()).isNull();
    }

    @Test
    void 결제를_대기_상태로_변경한다() {
        // given
        Payment payment = Payment.builder()
                .orderId(1L)
                .idempotencyKey("ORDER_1_1234567890")
                .amount(10000L)
                .paymentMethod(PaymentMethod.POINT)
                .status(PaymentStatus.APPROVED)
                .build();

        // when
        payment.markAsPending();

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void 결제_생성_시_ID가_null이면_자동_생성된다() {
        // given & when
        Payment payment = Payment.builder()
                .orderId(1L)
                .idempotencyKey("ORDER_1_1234567890")
                .amount(10000L)
                .paymentMethod(PaymentMethod.POINT)
                .status(PaymentStatus.PENDING)
                .build();

        // then
        assertThat(payment.getId()).isNull(); // JPA가 저장 시점에 ID를 생성하므로 null
    }

    @Test
    void 결제_생성_시_명시적으로_ID를_설정할_수_있다() {
        // given
        Long paymentId = 1L;

        // when
        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(1L)
                .idempotencyKey("ORDER_1_1234567890")
                .amount(10000L)
                .paymentMethod(PaymentMethod.POINT)
                .status(PaymentStatus.PENDING)
                .build();

        // then
        assertThat(payment.getId()).isEqualTo(paymentId);
    }
} 