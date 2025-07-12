package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.application.point.PointService;
import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.order.event.OrderEventPublisher;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentMethod;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.infrastructure.config.redis.DistributedLockService;
import kr.hhplus.be.server.infrastructure.external.orderinfo.DataPlatform;
import kr.hhplus.be.server.infrastructure.persistence.point.PointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static kr.hhplus.be.server.common.exception.ErrorCode.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PointRepository pointRepository;

    @MockitoBean
    private DataPlatform dataPlatform;

    @MockitoBean
    private DistributedLockService distributedLockService;

    @MockitoBean
    private PointService pointService;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    private Long orderId;
    private Long amount;
    private Long userId;
    private String idempotencyKey;

    private String generateIdempotencyKey(Long orderId) {
        return String.format("ORDER_%d_%d", orderId, System.currentTimeMillis());
    }

    @BeforeEach
    void setUp() {
        orderId = 1L;
        amount = 10000L;
        userId = 1L;
        idempotencyKey = generateIdempotencyKey(orderId);
        
        // 포인트 생성
        Point point = Point.builder()
            .userId(userId)
            .volume(50000L)
            .build();
        pointRepository.save(point);
        
        // 분산락 모킹 설정 - 락을 성공적으로 획득하고 작업을 실행하도록 설정
        lenient().when(distributedLockService.executePaymentLock(any(), any())).thenAnswer(invocation -> {
            return invocation.getArgument(1, java.util.function.Supplier.class).get();
        });
        lenient().when(distributedLockService.executePointLock(any(), any())).thenAnswer(invocation -> {
            return invocation.getArgument(1, java.util.function.Supplier.class).get();
        });
        
        // PointService 모킹 설정
        lenient().when(pointService.usePoint(any(), any())).thenReturn(point);
    }

    @Nested
    class ProcessPaymentTest {

        @Test
        void 결제를_성공적으로_처리한다() {
            // when
            paymentService.processPayment(orderId, userId, amount, PaymentMethod.POINT, idempotencyKey);

            // then
            // 결제 정보가 저장되었는지 확인
            Payment savedPayment = paymentRepository.findAll().stream()
                .filter(p -> p.getOrderId().equals(orderId))
                .findFirst()
                .orElseThrow();
            
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(savedPayment.getAmount()).isEqualTo(amount);
            assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.POINT);
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
        void 동일한_결제_요청이_중복으로_들어오면_예외가_발생한다() {
            // when - 첫 번째 결제 성공
            paymentService.processPayment(orderId, userId, amount, PaymentMethod.POINT, idempotencyKey);

            // then - 두 번째 결제는 중복으로 실패
            assertThatThrownBy(() -> paymentService.processPayment(orderId, userId, amount, PaymentMethod.POINT, idempotencyKey))
                    .isInstanceOf(ApiException.class)
                    .hasMessage(DUPLICATE_PAYMENT.getMessage());
        }
    }
} 