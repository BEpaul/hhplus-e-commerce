package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.common.exception.DuplicatePaymentException;
import kr.hhplus.be.server.common.exception.NotExistPaymentInfoException;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.infrastructure.external.DataPlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final DataPlatform dataPlatform;

    /**
     * 결제 처리
     * 1. 중복 결제 체크
     * 2. 외부 데이터 플랫폼에 결제 정보 전송
     * 3. 결제 성공/실패에 따른 상태 업데이트
     * 4. 결제 정보 저장
     */
    @Transactional
    public boolean processPayment(Payment payment) {
        if (payment == null) {
            throw new NotExistPaymentInfoException("결제 정보가 없습니다.");
        }

        checkDuplicatePayment(payment);
        boolean isPaymentSuccess = sendPaymentToDataPlatform(payment);
        updatePaymentStatus(payment, isPaymentSuccess);
        savePayment(payment);

        return isPaymentSuccess;
    }

    private void checkDuplicatePayment(Payment payment) {
        if (paymentRepository.existsByIdempotencyKey(payment.getIdempotencyKey())) {
            throw new DuplicatePaymentException("이미 처리된 결제 요청입니다.");
        }
    }

    private boolean sendPaymentToDataPlatform(Payment payment) {
        return dataPlatform.sendData(payment);
    }

    private void updatePaymentStatus(Payment payment, boolean isSuccess) {
        if (isSuccess) {
            payment.approve();
            return;
        }
        payment.cancel();
    }

    private void savePayment(Payment payment) {
        paymentRepository.save(payment);
    }
}
