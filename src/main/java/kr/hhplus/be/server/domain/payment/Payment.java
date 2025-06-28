package kr.hhplus.be.server.domain.payment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

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

    public static Payment create(Long orderId, PaymentMethod method, Long amount) {
        return Payment.builder()
                .orderId(orderId)
                .idempotencyKey(generateIdempotencyKey(orderId))
                .amount(amount)
                .paymentMethod(method)
                .status(PaymentStatus.PENDING)
                .build();
    }

    private static String generateIdempotencyKey(Long orderId) {
        return String.format("ORDER_%d_%d", orderId, System.currentTimeMillis());
    }

    public void approve() {
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    public void pending() {
        this.status = PaymentStatus.PENDING;
    }
} 