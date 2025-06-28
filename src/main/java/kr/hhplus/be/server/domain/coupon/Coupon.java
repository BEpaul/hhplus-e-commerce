package kr.hhplus.be.server.domain.coupon;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.ApiException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static kr.hhplus.be.server.common.exception.ErrorCode.NOT_SUPPORTED_DISCOUNT_TYPE;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long id;
    private Long discountValue;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;
    private String title;
    private Long stock;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Version
    private Long version;

    @Builder
    public Coupon(Long id, Long discountValue, DiscountType discountType, String title, Long stock, LocalDateTime startDate, LocalDateTime endDate, Long version) {
        this.id = id;
        this.discountValue = discountValue;
        this.discountType = discountType;
        this.title = title;
        this.stock = stock;
        this.startDate = startDate;
        this.endDate = endDate;
        this.version = version;
    }

    public Long apply(Long productPrice) {
        if (discountType == DiscountType.AMOUNT) {
            return Math.max(productPrice - discountValue, 0L);
        } else if (discountType == DiscountType.PERCENT) {
            return productPrice - (productPrice * discountValue / 100);
        }

        throw new ApiException(NOT_SUPPORTED_DISCOUNT_TYPE);
    }

    public void decreaseStock() {
        this.stock--;
    }
}
