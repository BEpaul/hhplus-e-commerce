package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.application.point.PointService;
import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentMethod;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.infrastructure.config.redis.DistributedLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static kr.hhplus.be.server.common.exception.ErrorCode.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PointService pointService;

    @Mock
    private DistributedLockService distributedLockService;

    private String generateIdempotencyKey(Long orderId) {
        return String.format("ORDER_%d_%d", orderId, System.currentTimeMillis());
    }

    @Nested
    class Describe_processPayment {
        private Long orderId;
        private Long userId;
        private Long amount;
        private Payment payment;
        private Point point;
        private String idempotencyKey;

        @BeforeEach
        void setUp() {
            orderId = 1L;
            userId = 1L;
            amount = 10000L;
            idempotencyKey = generateIdempotencyKey(orderId);
            payment = Payment.create(orderId, idempotencyKey, PaymentMethod.POINT, amount);
            point = Point.builder()
                .userId(userId)
                .volume(50000L)
                .build();

            // 분산락 모킹 설정 - 락을 성공적으로 획득하고 작업을 실행하도록 설정
            lenient().when(distributedLockService.executePaymentLock(any(), any())).thenAnswer(invocation -> {
                return invocation.getArgument(1, java.util.function.Supplier.class).get();
            });
            lenient().when(distributedLockService.executePointLock(any(), any())).thenAnswer(invocation -> {
                return invocation.getArgument(1, java.util.function.Supplier.class).get();
            });

            // 기본 모킹 설정
            lenient().when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
            lenient().when(paymentRepository.findById(any())).thenReturn(Optional.of(payment));
            lenient().when(paymentRepository.existsByIdempotencyKey(any())).thenReturn(false);
            lenient().when(pointService.usePoint(any(), any())).thenReturn(point);
        }

        @Test
        void 결제_정보_생성에_실패하면_예외가_발생한다() {
            // given
            Long invalidOrderId = null;
            String invalidIdempotencyKey = generateIdempotencyKey(invalidOrderId);

            // when & then
            assertThatThrownBy(() -> paymentService.processPayment(invalidOrderId, userId, amount, PaymentMethod.POINT, invalidIdempotencyKey))
                .isInstanceOf(ApiException.class)
                .hasMessage(PAYMENT_INFO_NOT_EXIST.getMessage());
        }

        @Test
        void 결제_처리가_성공하면_결제_상태를_APPROVED로_변경한다() {
            // when
            paymentService.processPayment(orderId, userId, amount, PaymentMethod.POINT, idempotencyKey);

            // then
            then(paymentRepository).should().save(any(Payment.class));
            then(pointService).should().usePoint(eq(userId), eq(amount));
            then(distributedLockService).should().executePaymentLock(eq(orderId), any());
            then(distributedLockService).should().executePointLock(eq(userId), any());
        }

        @Test
        void 중복_결제_요청이_들어오면_예외가_발생한다() {
            // given
            given(paymentRepository.existsByIdempotencyKey(any())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> paymentService.processPayment(orderId, userId, amount, PaymentMethod.POINT, idempotencyKey))
                .isInstanceOf(ApiException.class)
                .hasMessage(DUPLICATE_PAYMENT.getMessage());

            then(distributedLockService).should().executePaymentLock(eq(orderId), any());
        }
    }


}
