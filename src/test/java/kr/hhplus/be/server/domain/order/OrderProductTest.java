package kr.hhplus.be.server.domain.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OrderProductTest {

    @Test
    void 주문상품_엔티티를_성공적으로_생성한다() {
        // given
        Long productId = 1L;
        Long orderId = 1L;
        Long unitPrice = 10000L;
        Long quantity = 2L;

        // when
        OrderProduct orderProduct = OrderProduct.builder()
                .productId(productId)
                .orderId(orderId)
                .unitPrice(unitPrice)
                .quantity(quantity)
                .build();

        // then
        assertThat(orderProduct.getProductId()).isEqualTo(productId);
        assertThat(orderProduct.getOrderId()).isEqualTo(orderId);
        assertThat(orderProduct.getUnitPrice()).isEqualTo(unitPrice);
        assertThat(orderProduct.getQuantity()).isEqualTo(quantity);
    }

    @Test
    void 주문상품에_주문정보를_할당한다() {
        // given
        OrderProduct orderProduct = OrderProduct.builder()
                .productId(1L)
                .quantity(2L)
                .build();
        Long orderId = 1L;
        Long unitPrice = 10000L;

        // when
        orderProduct.assignOrderInfo(orderId, unitPrice);

        // then
        assertThat(orderProduct.getOrderId()).isEqualTo(orderId);
        assertThat(orderProduct.getUnitPrice()).isEqualTo(unitPrice);
    }

    @Test
    void 주문상품_생성_시_ID가_null이면_자동_생성된다() {
        // given & when
        OrderProduct orderProduct = OrderProduct.builder()
                .productId(1L)
                .quantity(2L)
                .build();

        // then
        assertThat(orderProduct.getId()).isNull(); // JPA가 저장 시점에 ID를 생성하므로 null
    }

    @Test
    void 주문상품_생성_시_명시적으로_ID를_설정할_수_있다() {
        // given
        Long orderProductId = 1L;

        // when
        OrderProduct orderProduct = OrderProduct.builder()
                .id(orderProductId)
                .productId(1L)
                .quantity(2L)
                .build();

        // then
        assertThat(orderProduct.getId()).isEqualTo(orderProductId);
    }
} 