package kr.hhplus.be.server.domain.order.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderProductEventDto {
    private Long productId;
    private String productName;
    private Long unitPrice;
    private Long quantity;
    private Long totalPrice;
}
