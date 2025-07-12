package kr.hhplus.be.server.domain.order.event;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.product.Product;
import lombok.Builder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class OrderCompletedEvent extends ApplicationEvent {

    private final Order order;
    private final List<OrderProduct> orderProducts;
    private final List<Product> products;

    @Builder
    private OrderCompletedEvent(Object source, Order order, List<OrderProduct> orderProducts, List<Product> products) {
        super(source);
        this.order = order;
        this.orderProducts = orderProducts;
        this.products = products;
    }

    public static OrderCompletedEvent of(Object source, Order order, List<OrderProduct> orderProducts, List<Product> products) {
        return OrderCompletedEvent.builder()
                .source(source)
                .order(order)
                .orderProducts(orderProducts)
                .products(products)
                .build();
    }
} 