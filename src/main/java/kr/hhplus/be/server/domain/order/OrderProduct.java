package kr.hhplus.be.server.domain.order;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_product_id")
    private Long id;
    private Long productId;
    private Long orderId;
    private Long unitPrice;
    private Long quantity;

    @Builder
    private OrderProduct(Long id, Long productId, Long orderId, Long unitPrice, Long quantity) {
        this.id = id;
        this.productId = productId;
        this.orderId = orderId;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public void assignOrderInfo(Long orderId, Long unitPrice) {
        this.orderId = orderId;
        this.unitPrice = unitPrice;
    }
}
