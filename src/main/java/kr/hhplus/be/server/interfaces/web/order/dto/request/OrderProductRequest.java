package kr.hhplus.be.server.interfaces.web.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import kr.hhplus.be.server.domain.order.OrderProduct;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderProductRequest {
    @NotNull(message = "상품 ID는 필수입니다.")
    @Min(value = 1, message = "상품 ID는 1 이상이어야 합니다.")
    private Long productId;

    @NotNull(message = "주문 수량은 필수입니다.")
    @Min(value = 1, message = "주문 수량은 1 이상이어야 합니다.")
    private Long quantity;

    public OrderProduct toOrderProduct() {
        return OrderProduct.builder()
                .productId(this.productId)
                .quantity(this.quantity)
                .build();
    }
} 