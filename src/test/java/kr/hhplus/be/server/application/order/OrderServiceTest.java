package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.bestseller.BestSellerRankingService;
import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.application.payment.PaymentService;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.order.OrderProductRepository;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.OrderStatus;
import kr.hhplus.be.server.domain.order.event.OrderEventPublisher;
import kr.hhplus.be.server.domain.payment.PaymentMethod;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.config.redis.DistributedLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static kr.hhplus.be.server.common.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
    private PaymentService paymentService;

    @Mock
    private DistributedLockService distributedLockService;

    @Mock
    private BestSellerRankingService bestSellerRankingService;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Nested
    class Describe_createOrder {

        private Order order;
        private List<OrderProduct> orderProducts;
        private Product product;
        private Map<Long, Product> productMap;

        @BeforeEach
        void setUp() {
            order = Order.builder()
                .id(1L)
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
                .id(1L)
                .name("상품명")
                .price(10000L)
                .stock(10L)
                .salesCount(0L)
                .description("상품 설명")
                .build();
            
            productMap = Map.of(1L, product);
            
            // 기본 모킹 설정
            lenient().when(productService.getProductWithPessimisticLock(1L)).thenReturn(product);
            lenient().when(productService.getProductMapByIds(any())).thenReturn(productMap);
            lenient().when(orderRepository.save(any(Order.class))).thenReturn(order);
            
            // 분산락 모킹 설정 - 락을 성공적으로 획득하고 작업을 실행하도록 설정
            lenient().when(distributedLockService.executeOrderLock(eq(1L), any())).thenAnswer(invocation -> {
                return invocation.getArgument(1, java.util.function.Supplier.class).get();
            });
            lenient().when(distributedLockService.executeProductStockLock(eq(1L), any())).thenAnswer(invocation -> {
                return invocation.getArgument(1, java.util.function.Supplier.class).get();
            });
            lenient().when(distributedLockService.executePaymentLock(any(), any())).thenAnswer(invocation -> {
                return invocation.getArgument(1, java.util.function.Supplier.class).get();
            });
            
            lenient().doNothing().when(bestSellerRankingService).incrementTodaySales(any(), any());
            
            lenient().doNothing().when(orderEventPublisher).publishOrderCompletedEvent(any(), any(), any());
        }

        @Test
        void 주문_상품이_없으면_예외가_발생한다() {
            assertThatThrownBy(() -> orderService.placeOrder(order, new ArrayList<>()))
                    .isInstanceOf(ApiException.class)
                    .hasMessage(ORDER_PRODUCT_EMPTY.getMessage());
        }

        @Test
        void 주문이_성공적으로_생성된다() {
            // given
            doNothing().when(paymentService).processPayment(eq(1L), eq(1L), eq(20000L), eq(PaymentMethod.POINT), any(String.class));

            // when
            Order result = orderService.placeOrder(order, orderProducts);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            then(orderRepository).should().save(order);
            then(orderProductRepository).should().save(any(OrderProduct.class));
            then(paymentService).should().processPayment(eq(1L), eq(1L), eq(20000L), eq(PaymentMethod.POINT), any(String.class));
            then(distributedLockService).should().executeOrderLock(eq(1L), any());
            then(distributedLockService).should().executeProductStockLock(eq(1L), any());
            then(bestSellerRankingService).should().incrementTodaySales(eq(1L), eq(2L));
            then(orderEventPublisher).should().publishOrderCompletedEvent(any(Order.class), eq(orderProducts), any());
        }

        @Test
        void 쿠폰이_적용된_주문이_성공적으로_생성된다() {
            // given
            Order couponOrder = Order.builder()
                .id(1L)
                .userId(1L)
                .userCouponId(1L)
                .build();
            given(couponService.calculateDiscountPrice(1L, 20000L)).willReturn(15000L);
            doNothing().when(paymentService).processPayment(eq(1L), eq(1L), eq(15000L), eq(PaymentMethod.POINT), any(String.class));

            // when
            Order result = orderService.placeOrder(couponOrder, orderProducts);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            then(couponService).should().useCoupon(1L);
            then(paymentService).should().processPayment(eq(1L), eq(1L), eq(15000L), eq(PaymentMethod.POINT), any(String.class));
            then(distributedLockService).should().executeOrderLock(eq(1L), any());
            then(distributedLockService).should().executeProductStockLock(eq(1L), any());
            then(bestSellerRankingService).should().incrementTodaySales(eq(1L), eq(2L));
            then(orderEventPublisher).should().publishOrderCompletedEvent(any(Order.class), eq(orderProducts), any());
        }

        @Test
        void 결제가_실패하면_예외가_발생한다() {
            // given
            doThrow(new ApiException(PAYMENT_FAILED))
                .when(paymentService).processPayment(eq(1L), eq(1L), eq(20000L), eq(PaymentMethod.POINT), any(String.class));

            // when & then
            assertThatThrownBy(() -> orderService.placeOrder(order, orderProducts))
                    .isInstanceOf(ApiException.class)
                    .hasMessage(PAYMENT_FAILED.getMessage());
        }
    }
} 