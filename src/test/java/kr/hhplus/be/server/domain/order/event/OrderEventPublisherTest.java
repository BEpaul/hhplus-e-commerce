package kr.hhplus.be.server.domain.order.event;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.order.OrderStatus;
import kr.hhplus.be.server.domain.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderEventPublisher orderEventPublisher;

    @Nested
    class Describe_publishOrderCompletedEvent {

        private Order order;
        private List<OrderProduct> orderProducts;
        private List<Product> products;

        @BeforeEach
        void setUp() {
            order = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .totalAmount(1000L)
                    .build();

            orderProducts = Arrays.asList(
                    OrderProduct.builder().productId(1L).quantity(1L).unitPrice(1000L).build()
            );

            products = Arrays.asList(
                    Product.builder().id(1L).name("상품1").price(1000L).stock(5L).salesCount(0L).description("상품1").build()
            );
        }

        @Test
        void 주문_완료_이벤트_발행에_성공한다() {
            // when
            orderEventPublisher.publishOrderCompletedEvent(order, orderProducts, products);

            // then
            verify(eventPublisher, times(1)).publishEvent(any(OrderCompletedEvent.class));
        }

        @Test
        void 이벤트_발행_실패_시_예외를_로그로_기록한다() {
            // given
            doThrow(new RuntimeException("이벤트 발행 실패")).when(eventPublisher).publishEvent(any());

            // when
            orderEventPublisher.publishOrderCompletedEvent(order, orderProducts, products);

            // then
            verify(eventPublisher, times(1)).publishEvent(any(OrderCompletedEvent.class));
        }

        @Test
        void 여러_상품이_있는_경우_이벤트가_정상_발행된다() {
            // given
            List<OrderProduct> multipleOrderProducts = Arrays.asList(
                    OrderProduct.builder().productId(1L).quantity(1L).unitPrice(1000L).build(),
                    OrderProduct.builder().productId(2L).quantity(2L).unitPrice(2000L).build()
            );

            List<Product> multipleProducts = Arrays.asList(
                    Product.builder().id(1L).name("상품1").price(1000L).stock(5L).salesCount(0L).description("상품1").build(),
                    Product.builder().id(2L).name("상품2").price(2000L).stock(10L).salesCount(0L).description("상품2").build()
            );

            // when
            orderEventPublisher.publishOrderCompletedEvent(order, multipleOrderProducts, multipleProducts);

            // then
            verify(eventPublisher, times(1)).publishEvent(any(OrderCompletedEvent.class));
        }
    }
} 