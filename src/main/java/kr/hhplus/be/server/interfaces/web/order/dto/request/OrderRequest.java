package kr.hhplus.be.server.interfaces.web.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OrderRequest {
    @NotNull(message = "사용자 ID는 필수입니다.")
    @Min(value = 1, message = "사용자 ID는 1 이상이어야 합니다.")
    private Long userId;

    @Min(value = 1, message = "쿠폰 ID는 1 이상이어야 합니다.")
    private Long userCouponId;

    @NotNull(message = "주문 상품 목록은 필수입니다.")
    @Size(min = 1, message = "주문 상품은 최소 1개 이상이어야 합니다.")
    @Valid
    private List<OrderProductRequest> orderProducts;

    public Order toOrder() {
        return Order.builder()
                .userId(this.userId)
                .userCouponId(this.userCouponId)
                .build();
    }

    public List<OrderProduct> toOrderProducts() {
        return this.orderProducts.stream()
                .map(OrderProductRequest::toOrderProduct)
                .toList();
    }
} 