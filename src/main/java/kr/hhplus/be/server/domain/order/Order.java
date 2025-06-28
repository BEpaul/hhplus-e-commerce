package kr.hhplus.be.server.domain.order;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.ApiException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static kr.hhplus.be.server.common.exception.ErrorCode.ALREADY_APPLIED_COUPON;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;
    private Long userId;
    private Long userCouponId;
    private Long totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private boolean isCouponApplied;

    @Builder
    private Order(Long id, Long userId, Long userCouponId, Long totalAmount, boolean isCouponApplied) {
       this.id = id;
       this.userId = userId;
       this.userCouponId = userCouponId;
       this.totalAmount = totalAmount;
       this.status = OrderStatus.WAITING;
       this.isCouponApplied = isCouponApplied;
    }

    public void calculateTotalAmount(Long totalAmount) {
       this.totalAmount = totalAmount;
    }

    public void applyCoupon() {
        if (this.isCouponApplied) {
            throw new ApiException(ALREADY_APPLIED_COUPON);
        }
        this.isCouponApplied = true;
    }

    public void success() {
        this.status = OrderStatus.COMPLETED;
    }

    public void fail() {
        this.status = OrderStatus.FAILED;
    }
}