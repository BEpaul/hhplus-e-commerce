package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentMethod;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.infrastructure.external.DataPlatform;
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
import static org.mockito.BDDMockito.given;
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

    private Long orderId;
    private Long amount;
    private Long userId;

    @BeforeEach
    void setUp() {
        orderId = 1L;
        amount = 10000L;
        userId = 1L;
        // 포인트 생성
        Point point = Point.builder()
            .userId(userId)
            .volume(50000L)
            .build();
        pointRepository.save(point);
    }

    @Nested
    class ProcessPaymentTest {

        @Test
        void 결제를_성공적으로_처리한다() {
            // given
            Payment payment = Payment.create(orderId, PaymentMethod.POINT, amount);
            given(dataPlatform.sendData(any())).willReturn(true);

            // when
            paymentService.processPayment(payment, userId);

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            // 포인트 차감 검증
            Point updated = pointRepository.findByUserId(userId).orElseThrow();
            assertThat(updated.getVolume()).isEqualTo(40000L);
        }

        @Test
        void 결제_정보가_없으면_예외가_발생한다() {
            // given
            Payment payment = null;

            // when & then
            assertThatThrownBy(() -> paymentService.processPayment(payment, userId))
                    .isInstanceOf(ApiException.class)
                    .hasMessage(PAYMENT_INFO_NOT_EXIST.getMessage());
        }

        @Test
        void 외부_결제_플랫폼_응답이_실패하면_결제가_취소되고_예외가_발생한다() {
            // given
            Payment payment = Payment.create(orderId, PaymentMethod.POINT, amount);
            given(dataPlatform.sendData(any())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> paymentService.processPayment(payment, userId))
                    .isInstanceOf(ApiException.class)
                    .hasMessage(PAYMENT_PROCESSING_FAILED.getMessage());
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        }

        @Test
        void 동일한_결제_요청이_중복으로_들어오면_예외가_발생한다() {
            // given
            Payment payment = Payment.create(orderId, PaymentMethod.POINT, amount);
            given(dataPlatform.sendData(any())).willReturn(true);

            // when
            paymentService.processPayment(payment, userId);

            // then
            assertThatThrownBy(() -> paymentService.processPayment(payment, userId))
                    .isInstanceOf(ApiException.class)
                    .hasMessage(DUPLICATE_PAYMENT.getMessage());
        }
    }
} 