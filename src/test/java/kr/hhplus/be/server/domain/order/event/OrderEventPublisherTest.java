package kr.hhplus.be.server.domain.order.event;

import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
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

import static kr.hhplus.be.server.common.exception.ErrorCode.PRODUCT_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ProductService productService;

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
                    .totalAmount(10000L)
                    .build();
            order.success();

            orderProducts = Arrays.asList(
                    OrderProduct.builder()
                            .productId(1L)
                            .quantity(2L)
                            .unitPrice(5000L)
                            .build()
            );

            products = Arrays.asList(
                    Product.builder()
                            .id(1L)
                            .name("테스트 상품")
                            .price(5000L)
                            .stock(10L)
                            .salesCount(0L)
                            .description("테스트 상품입니다.")
                            .build()
            );
        }

        @Test
        void 주문_완료_이벤트_발행에_성공한다() {
            // given
            when(productService.getProduct(1L)).thenReturn(products.get(0));

            // when
            orderEventPublisher.publishOrderCompletedEvent(order, orderProducts);

            // then
            verify(productService, times(1)).getProduct(1L);
            verify(eventPublisher, times(1)).publishEvent(any(OrderCompletedEvent.class));
        }

        @Test
        void 상품_조회_실패_시_이벤트_발행이_실패한다() {
            // given
            when(productService.getProduct(1L)).thenThrow(new ApiException(PRODUCT_NOT_FOUND));

            // when
            orderEventPublisher.publishOrderCompletedEvent(order, orderProducts);

            // then
            verify(productService, times(1)).getProduct(1L);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void 여러_상품이_있는_경우_모든_상품을_조회한다() {
            // given
            List<OrderProduct> multipleOrderProducts = Arrays.asList(
                    OrderProduct.builder().productId(1L).quantity(1L).unitPrice(1000L).build(),
                    OrderProduct.builder().productId(2L).quantity(2L).unitPrice(2000L).build()
            );

            Product product1 = Product.builder().id(1L).name("상품1").price(1000L).stock(5L).salesCount(0L).description("상품1").build();
            Product product2 = Product.builder().id(2L).name("상품2").price(2000L).stock(10L).salesCount(0L).description("상품2").build();

            when(productService.getProduct(1L)).thenReturn(product1);
            when(productService.getProduct(2L)).thenReturn(product2);

            // when
            orderEventPublisher.publishOrderCompletedEvent(order, multipleOrderProducts);

            // then
            verify(productService, times(1)).getProduct(1L);
            verify(productService, times(1)).getProduct(2L);
            verify(eventPublisher, times(1)).publishEvent(any(OrderCompletedEvent.class));
        }
    }
} 