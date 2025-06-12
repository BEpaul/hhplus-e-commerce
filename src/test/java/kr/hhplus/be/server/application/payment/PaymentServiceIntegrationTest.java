package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.common.exception.DuplicatePaymentException;
import kr.hhplus.be.server.common.exception.NotExistPaymentInfoException;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentMethod;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
import kr.hhplus.be.server.infrastructure.external.DataPlatform;
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

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private DataPlatform dataPlatform;

    private Long orderId;
    private Long amount;

    @BeforeEach
    void setUp() {
        orderId = 1L;
        amount = 10000L;
    }

    @Nested
    class ProcessPaymentTest {

        @Test
        void 결제를_성공적으로_처리한다() {
            // given
            Payment payment = Payment.create(orderId, PaymentMethod.POINT, amount);
            given(dataPlatform.sendData(any())).willReturn(true);

            // when
            boolean result = paymentService.processPayment(payment);

            // then
            assertThat(result).isTrue();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        }

        @Test
        void 결제_정보가_없으면_예외가_발생한다() {
            // given
            Payment payment = null;

            // when & then
            assertThatThrownBy(() -> paymentService.processPayment(payment))
                    .isInstanceOf(NotExistPaymentInfoException.class);
        }

        @Test
        void 외부_결제_플랫폼_응답이_실패하면_결제가_취소된다() {
            // given
            Payment payment = Payment.create(orderId, PaymentMethod.POINT, amount);
            given(dataPlatform.sendData(any())).willReturn(false);

            // when
            boolean result = paymentService.processPayment(payment);

            // then
            assertThat(result).isFalse();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        }

        @Test
        void 동일한_결제_요청이_중복으로_들어오면_예외가_발생한다() {
            // given
            Payment payment = Payment.create(orderId, PaymentMethod.POINT, amount);
            given(dataPlatform.sendData(any())).willReturn(true);

            // when
            paymentService.processPayment(payment);

            // then
            assertThatThrownBy(() -> paymentService.processPayment(payment))
                    .isInstanceOf(DuplicatePaymentException.class);
        }
    }
} 