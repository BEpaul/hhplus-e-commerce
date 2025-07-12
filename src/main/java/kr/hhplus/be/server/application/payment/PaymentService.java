package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.application.point.PointService;
import kr.hhplus.be.server.common.exception.*;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentMethod;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.infrastructure.config.redis.DistributedLockService;
import kr.hhplus.be.server.infrastructure.external.orderinfo.DataPlatform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * 1. 결제 정보 생성
     * 2. 주문별 분산락 적용 (중복 결제 방지)
     * 3. 중복 결제 체크 및 결제 정보 저장
     * 4. 포인트 차감 및 외부 플랫폼 결제 처리
     */
    @Transactional
    public void processPayment(Long orderId, Long userId, Long totalAmount, PaymentMethod paymentMethod, String idempotencyKey) {
        Payment payment = Payment.create(orderId, idempotencyKey, paymentMethod, totalAmount);

        if (payment == null) {
            throw new ApiException(PAYMENT_INFO_NOT_EXIST);
        }

        // 주문별 분산락 적용 (같은 주문의 중복 결제 방지)
        distributedLockService.executePaymentLock(orderId, () -> {
            log.info("결제 처리 시작 - 주문 ID: {}, 사용자 ID: {}", orderId, userId);
            
            Payment savedPayment = validateAndSavePayment(orderId, payment);
            processPaymentWithLock(savedPayment, userId);
            
            return null;
        });
    }

    /**
     * 결제 검증 및 저장
     */
    private Payment validateAndSavePayment(Long orderId, Payment payment) {
        checkDuplicatePayment(orderId, payment);
        return savePayment(payment);
    }

    /**
     * 중복 결제 체크
     */
    private void checkDuplicatePayment(Long orderId, Payment payment) {
        if (paymentRepository.existsByIdempotencyKey(payment.getIdempotencyKey())) {
            log.warn("중복 결제 감지 - 주문 ID: {}, 중복 방지 키: {}", orderId, payment.getIdempotencyKey());
            throw new ApiException(DUPLICATE_PAYMENT);
        }
    }

    /**
     * 결제별 분산락을 적용한 결제 처리
     */
    private void processPaymentWithLock(Payment payment, Long userId) {
        distributedLockService.executePaymentLock(payment.getId(), () -> {
            deductPoint(payment, userId);
            payment.markAsApproved();
            log.info("결제 처리 완료 - 결제 ID: {}", payment.getId());
            return null;
        });
    }

    /**
     * 포인트 차감 (사용자별 분산락 적용)
     */
    private void deductPoint(Payment payment, Long userId) {
        if (payment.getPaymentMethod() == PaymentMethod.POINT) {
            distributedLockService.executePointLock(userId, () -> {
                pointService.usePoint(userId, payment.getAmount());
                log.debug("포인트 차감 완료 - 사용자 ID: {}, 차감 금액: {}", userId, payment.getAmount());
                return null;
            });
        }
    }

    private Payment savePayment(Payment payment) {
        return paymentRepository.save(payment);
    }
}
