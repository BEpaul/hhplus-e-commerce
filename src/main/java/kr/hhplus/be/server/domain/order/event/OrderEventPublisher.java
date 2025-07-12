package kr.hhplus.be.server.domain.order.event;

import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.product.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final ProductService productService;

    /**
     * 주문 완료 이벤트 발행
     */
    public void publishOrderCompletedEvent(Order order, List<OrderProduct> orderProducts) {
        try {
            List<Product> products = getProductsByOrderProducts(orderProducts);
            
            OrderCompletedEvent event = OrderCompletedEvent.of(this, order, orderProducts, products);
            eventPublisher.publishEvent(event);
            
            log.info("주문 완료 이벤트 발행 완료 - 주문 ID: {}, 사용자 ID: {}", 
                order.getId(), order.getUserId());
                
        } catch (Exception e) {
            log.error("주문 완료 이벤트 발행 실패 - 주문 ID: {}", order.getId(), e);
        }
    }

    /**
     * 주문 상품에 해당하는 상품 정보 조회
     */
    private List<Product> getProductsByOrderProducts(List<OrderProduct> orderProducts) {
        List<Long> productIds = orderProducts.stream()
                .map(OrderProduct::getProductId)
                .toList();
        
        return productIds.stream()
                .map(productService::getProduct)
                .collect(Collectors.toList());
    }
} 