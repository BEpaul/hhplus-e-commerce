package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.common.exception.NotExistPaymentInfoException;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentMethod;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.infrastructure.external.DataPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private DataPlatform dataPlatform;

    @Nested
    class Describe_processPayment {

        private Payment payment;

        @BeforeEach
        void setUp() {
            payment = Payment.create(1L, PaymentMethod.POINT, 10000L);
        }

        @Test
        void 결제_정보가_없으면_예외가_발생한다() {
            assertThatThrownBy(() -> paymentService.processPayment(null))
                .isInstanceOf(NotExistPaymentInfoException.class)
                .hasMessage("결제 정보가 없습니다.");
        }

        @Test
        void 결제가_성공하면_true를_반환하고_결제_상태를_APPROVED로_변경한다() {
            // given
            given(dataPlatform.sendData(payment)).willReturn(true);

            // when
            boolean result = paymentService.processPayment(payment);

            // then
            assertThat(result).isTrue();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            then(paymentRepository).should().save(payment);
        }

        @Test
        void 결제가_실패하면_false를_반환하고_결제_상태를_CANCELED로_변경한다() {
            // given
            given(dataPlatform.sendData(payment)).willReturn(false);

            // when
            boolean result = paymentService.processPayment(payment);

            // then
            assertThat(result).isFalse();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
            then(paymentRepository).should().save(payment);
        }
    }
}
