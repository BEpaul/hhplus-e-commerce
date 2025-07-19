package kr.hhplus.be.server.domain.order.event;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.product.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주문 완료 이벤트 발행
     */
    public void publishOrderCompletedEvent(Order order, List<OrderProduct> orderProducts, List<Product> products) {
        try {
            OrderCompletedEvent event = OrderCompletedEvent.of(this, order, orderProducts, products);
            eventPublisher.publishEvent(event);
            
            log.info("주문 완료 이벤트 발행 완료 - 주문 ID: {}, 사용자 ID: {}", 
                order.getId(), order.getUserId());
                
        } catch (Exception e) {
            log.error("주문 완료 이벤트 발행 실패 - 주문 ID: {}", order.getId(), e);
        }
    }
} 