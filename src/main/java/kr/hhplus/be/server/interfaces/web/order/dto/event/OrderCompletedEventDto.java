package kr.hhplus.be.server.interfaces.web.order.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import kr.hhplus.be.server.domain.order.event.OrderCompletedEvent;
import kr.hhplus.be.server.domain.product.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEventDto {
    private Long orderId;
    private Long userId;
    private Long totalAmount;
    private String orderStatus;
    private List<OrderProductEventDto> orderProducts;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    public static OrderCompletedEventDto of(Long orderId, Long userId, Long totalAmount,
                                            String orderStatus, List<OrderProductEventDto> orderProducts) {
        return OrderCompletedEventDto.builder()
                .orderId(orderId)
                .userId(userId)
                .totalAmount(totalAmount)
                .orderStatus(orderStatus)
                .orderProducts(orderProducts)
                .completedAt(LocalDateTime.now())
                .build();
    }

    public static OrderCompletedEventDto from(OrderCompletedEvent event) {
        List<OrderProductEventDto> orderProductEvents = createOrderProductEvents(event);
        
        return OrderCompletedEventDto.builder()
                .orderId(event.getOrder().getId())
                .userId(event.getOrder().getUserId())
                .totalAmount(event.getOrder().getTotalAmount())
                .orderStatus(event.getOrder().getStatus().name())
                .orderProducts(orderProductEvents)
                .completedAt(LocalDateTime.now())
                .build();
    }

    private static List<OrderProductEventDto> createOrderProductEvents(OrderCompletedEvent event) {
        return event.getOrderProducts().stream()
                .map(orderProduct -> {
                    Product product = findProductById(event.getProducts(), orderProduct.getProductId());
                    
                    return OrderProductEventDto.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .unitPrice(orderProduct.getUnitPrice())
                            .quantity(orderProduct.getQuantity())
                            .totalPrice(orderProduct.getUnitPrice() * orderProduct.getQuantity())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private static Product findProductById(List<Product> products, Long productId) {
        return products.stream()
                .filter(p -> p.getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + productId));
    }
} 