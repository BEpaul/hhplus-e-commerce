package kr.hhplus.be.server.domain.coupon.event;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.config.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "coupon_outbox_event")
public class CouponOutBoxEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponOutBoxEventStatus status;

    @Column
    private Long retryCount;

    @Builder
    public CouponOutBoxEvent(Long id, String eventType, String payload, CouponOutBoxEventStatus status, Long retryCount) {
        this.id = id;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status != null ? status : CouponOutBoxEventStatus.PENDING;
        this.retryCount = retryCount != null ? retryCount : 0L;
    }

    public void markAsProcessed() {
        this.status = CouponOutBoxEventStatus.PROCESSED;
    }

    public void markAsFailed() {
        this.status = CouponOutBoxEventStatus.FAILED;
        this.retryCount++;
    }

    public boolean canRetry() {
        return this.retryCount < 3; // 최대 3번 재시도
    }
} 