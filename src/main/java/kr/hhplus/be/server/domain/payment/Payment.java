package kr.hhplus.be.server.domain.payment;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.ApiException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

import static kr.hhplus.be.server.common.exception.ErrorCode.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;
    
    private Long orderId;
    private String idempotencyKey;
    private Long amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    private LocalDateTime approvedAt;
    private LocalDateTime canceledAt;

    @Builder
    private Payment(Long id, Long orderId, String idempotencyKey, Long amount, PaymentMethod paymentMethod, PaymentStatus status) {
        this.id = id;
        this.orderId = orderId;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
    }

    public static Payment create(Long orderId, String idempotencyKey, PaymentMethod method, Long amount) {
        if (orderId == null) {
            throw new ApiException(PAYMENT_INFO_NOT_EXIST);
        }

        return Payment.builder()
                .orderId(orderId)
                .idempotencyKey(idempotencyKey)
                .amount(amount)
                .paymentMethod(method)
                .status(PaymentStatus.PENDING)
                .build();
    }

    public void markAsApproved() {
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void markAsCanceled() {
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    public void markAsPending() {
        this.status = PaymentStatus.PENDING;
    }
} 