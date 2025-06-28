package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.application.point.PointService;
import kr.hhplus.be.server.common.exception.*;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentMethod;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.infrastructure.config.redis.DistributedLockService;
import kr.hhplus.be.server.infrastructure.external.DataPlatform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static kr.hhplus.be.server.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final DataPlatform dataPlatform;
    private final PointService pointService;
    private final DistributedLockService distributedLockService;

    /**
     * 결제 처리
     * 1. 결제별 분산락 획득
     * 2. 중복 결제 체크
     * 3. 포인트 차감
     * 4. 결제 정보 저장
     * 5. 외부 플랫폼 결제 처리 시작
     */
    @Transactional
    public void processPayment(Payment payment, Long userId) {
        if (payment == null) {
            throw new ApiException(PAYMENT_INFO_NOT_EXIST);
        }

        // 결제별 분산락 적용
        distributedLockService.executePaymentLock(payment.getId(), () -> {
            log.info("결제 처리 시작 - 결제 ID: {}, 사용자 ID: {}", payment.getId(), userId);
            
            checkDuplicatePayment(payment);
            deductPointWithLock(payment, userId);

            // 결제 정보를 저장 (이미 PENDING 상태)
            savePayment(payment);

            // 외부 플랫폼 결제 처리 시작
            handleExternalPayment(payment.getId());
            
            log.info("결제 처리 완료 - 결제 ID: {}", payment.getId());
            return null;
        });
    }

    private void checkDuplicatePayment(Payment payment) {
        if (paymentRepository.existsByIdempotencyKey(payment.getIdempotencyKey())) {
            log.warn("중복 결제 감지 - 결제 ID: {}, 중복 방지 키: {}", payment.getId(), payment.getIdempotencyKey());
            throw new ApiException(DUPLICATE_PAYMENT);
        }
    }

    /**
     * 사용자별 분산락을 적용한 포인트 차감
     */
    private void deductPointWithLock(Payment payment, Long userId) {
        if (payment.getPaymentMethod() == PaymentMethod.POINT) {
            distributedLockService.executePointLock(userId, () -> {
                pointService.usePoint(userId, payment.getAmount());
                log.debug("포인트 차감 완료 - 사용자 ID: {}, 차감 금액: {}", userId, payment.getAmount());
                return null;
            });
        }
    }

//    @Async("taskExecutor")
    protected void handleExternalPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(PAYMENT_INFO_NOT_EXIST));

        boolean isPaymentSuccess = sendPaymentToDataPlatform(payment);

        if (!isPaymentSuccess) {
            payment.cancel();
            savePayment(payment);
            log.error("결제 처리 실패 - 결제 ID: {}", paymentId);
            throw new ApiException(PAYMENT_PROCESSING_FAILED);
        }

        payment.approve();
        savePayment(payment);

        log.info("결제 처리 완료 - 결제 ID: {}, 성공 여부: {}", paymentId, isPaymentSuccess);
    }

    private boolean sendPaymentToDataPlatform(Payment payment) {
        return dataPlatform.sendData(payment);
    }

    private void savePayment(Payment payment) {
        paymentRepository.save(payment);
    }
}
