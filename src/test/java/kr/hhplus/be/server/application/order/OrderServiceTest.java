package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.application.payment.PaymentService;
import kr.hhplus.be.server.application.point.PointService;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.common.exception.FailedPaymentException;
import kr.hhplus.be.server.common.exception.OrderProductEmptyException;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.order.OrderProductRepository;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.OrderStatus;
import kr.hhplus.be.server.domain.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderProductRepository orderProductRepository;

    @Mock
    private CouponService couponService;

    @Mock
    private ProductService productService;

    @Mock
    private PointService pointService;

    @Mock
    private PaymentService paymentService;

    @Nested
    class Describe_createOrder {

        private Order order;
        private List<OrderProduct> orderProducts;
        private Product product;

        @BeforeEach
        void setUp() {
            order = Order.builder()
                .userId(1L)
                .userCouponId(null)
                .build();
            orderProducts = new ArrayList<>();
            OrderProduct orderProduct = OrderProduct.builder()
                .productId(1L)
                .quantity(2L)
                .build();
            orderProducts.add(orderProduct);

            product = Product.builder()
                .name("상품명")
                .price(10000L)
                .stock(10L)
                .description("상품 설명")
                .build();
            lenient().when(productService.getProduct(1L)).thenReturn(product);
            lenient().when(orderRepository.save(any(Order.class))).thenReturn(order);
        }

        @Test
        void 주문_상품이_없으면_예외가_발생한다() {
            assertThatThrownBy(() -> orderService.createOrder(order, new ArrayList<>()))
                    .isInstanceOf(OrderProductEmptyException.class);
        }

        @Test
        void 주문이_성공적으로_생성된다() {
            // given
            given(paymentService.processPayment(any())).willReturn(true);

            // when
            Order result = orderService.createOrder(order, orderProducts);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(OrderStatus.DONE);
            then(orderRepository).should().save(order);
            then(orderProductRepository).should().save(any(OrderProduct.class));
            then(pointService).should().usePoint(order.getUserId(), 20000L);
        }

        @Test
        void 쿠폰이_적용된_주문이_성공적으로_생성된다() {
            // given
            order = Order.builder()
                .userId(1L)
                .userCouponId(1L)
                .build();
            given(couponService.calculateDiscountPrice(1L, 20000L)).willReturn(15000L);
            given(paymentService.processPayment(any())).willReturn(true);

            // when
            Order result = orderService.createOrder(order, orderProducts);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(OrderStatus.DONE);
            then(couponService).should().useCoupon(1L);
            then(pointService).should().usePoint(order.getUserId(), 15000L);
        }

        @Test
        void 결제가_실패하면_예외가_발생한다() {
            // given
            given(paymentService.processPayment(any())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(order, orderProducts))
                    .isInstanceOf(FailedPaymentException.class);
        }
    }
} 