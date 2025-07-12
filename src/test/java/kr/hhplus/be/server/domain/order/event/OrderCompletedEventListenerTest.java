package kr.hhplus.be.server.domain.order.event;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.external.orderinfo.DataPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCompletedEventListenerTest {

    @Mock
    private DataPlatform dataPlatform;

    @InjectMocks
    private OrderCompletedEventListener listener;

    private Order order;
    private List<OrderProduct> orderProducts;
    private List<Product> products;
    private OrderCompletedEvent event;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(1L)
                .userId(1L)
                .totalAmount(10000L)
                .build();
        order.success(); // COMPLETED 상태로 변경

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

        event = OrderCompletedEvent.of(this, order, orderProducts, products);
    }

    @Test
    void 주문_완료_시_이벤트_처리에_성공한다() {
        // given
        when(dataPlatform.sendOrderData(any())).thenReturn(true);

        // when
        listener.handleOrderCompletedEvent(event);

        // then
        verify(dataPlatform, times(1)).sendOrderData(any());
    }

    @Test
    void 주문_완료했지만_이벤트_처리에_실패하여_데이터_플랫폼_전송에_실패한다() {
        // given
        when(dataPlatform.sendOrderData(any())).thenReturn(false);

        // when
        listener.handleOrderCompletedEvent(event);

        // then
        verify(dataPlatform, times(1)).sendOrderData(any());
    }

    @Test
    void 주문_완료_이벤트_처리에_실패하면_예외가_발생한다() {
        // given
        when(dataPlatform.sendOrderData(any())).thenThrow(new RuntimeException("데이터 플랫폼 오류"));

        // when
        listener.handleOrderCompletedEvent(event);

        // then
        verify(dataPlatform, times(1)).sendOrderData(any());
    }
} 