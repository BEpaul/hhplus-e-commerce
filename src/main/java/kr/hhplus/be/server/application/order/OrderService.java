package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.application.payment.PaymentService;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.order.OrderProductRepository;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentMethod;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.config.redis.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static kr.hhplus.be.server.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final CouponService couponService;
    private final ProductService productService;
    private final PaymentService paymentService;
    private final DistributedLockService distributedLockService;

    /**
     * 주문 생성
     * 1. 사용자별 주문 락 획득
     * 2. 주문 상품 유효성 검사
     * 3. 상품 재고 감소
     * 4. 총 가격 계산
     * 5. 쿠폰 할인 적용
     * 6. 쿠폰 사용 처리
     * 7. 주문 정보 저장
     * 8. 결제 처리 (포인트 차감)
     * 9. 주문 상품 정보 저장
     * 10. 주문 상태 업데이트
     */
    @Transactional
    public Order createOrder(Order order, List<OrderProduct> orderProducts) {
        return distributedLockService.executeOrderLock(order.getUserId(), () -> {
            log.info("주문 생성 시작 - 사용자 ID: {}", order.getUserId());
            
            validateOrderProducts(orderProducts);
            decreaseProductStocksWithLock(orderProducts);
            long totalPrice = calculateTotalPrice(orderProducts);
            totalPrice = calculateDiscountedPrice(order, totalPrice);
            processCouponUsage(order);

            Order savedOrder = saveOrder(order, totalPrice);
            processPayment(savedOrder, totalPrice);
            saveOrderProducts(savedOrder, orderProducts);

            log.info("주문 생성 완료 - 주문 ID: {}, 사용자 ID: {}", savedOrder.getId(), order.getUserId());
            return savedOrder;
        });
    }

    private void validateOrderProducts(List<OrderProduct> orderProducts) {
        if (orderProducts == null || orderProducts.isEmpty()) {
            throw new ApiException(ORDER_PRODUCT_EMPTY);
        }
    }

    /**
     * 재고 감소
     */
    private void decreaseProductStocksWithLock(List<OrderProduct> orderProducts) {
        for (OrderProduct orderProduct : orderProducts) {
            distributedLockService.executeProductStockLock(orderProduct.getProductId(), () -> {
                Product product = productService.getProductWithPessimisticLock(orderProduct.getProductId());
                product.decreaseStock(orderProduct.getQuantity());
                log.debug("상품 재고 감소 - 상품 ID: {}, 수량: {}", orderProduct.getProductId(), orderProduct.getQuantity());
                return null;
            });
        }
    }

    private long calculateTotalPrice(List<OrderProduct> orderProducts) {
        return orderProducts.stream()
                .mapToLong(it -> {
                    Product product = productService.getProduct(it.getProductId());
                    return product.getPrice() * it.getQuantity();
                })
                .sum();
    }

    private long calculateDiscountedPrice(Order order, long totalPrice) {
        if (order.getUserCouponId() == null) {
            return totalPrice;
        }
        return couponService.calculateDiscountPrice(order.getUserCouponId(), totalPrice);
    }

    private void processCouponUsage(Order order) {
        if (order.getUserCouponId() == null) {
            return;
        }
        couponService.useCoupon(order.getUserCouponId());
        order.applyCoupon();
    }

    private Order saveOrder(Order order, long totalPrice) {
        order.calculateTotalAmount(totalPrice);
        return orderRepository.save(order);
    }

    /**
     * 결제 처리
     */
    private void processPayment(Order order, long totalPrice) {
        try {
            Payment payment = Payment.create(order.getId(), PaymentMethod.POINT, totalPrice);
            
            // 결제별 분산락 적용
            distributedLockService.executePaymentLock(payment.getId(), () -> {
                paymentService.processPayment(payment, order.getUserId());
                return null;
            });
            
            order.success();
            log.info("결제 처리 완료 - 주문 ID: {}, 결제 ID: {}", order.getId(), payment.getId());
        } catch (Exception e) {
            order.fail();
            log.error("결제 처리 실패 - 주문 ID: {}", order.getId(), e);
            throw new ApiException(PAYMENT_FAILED);
        }
    }

    private void saveOrderProducts(Order order, List<OrderProduct> orderProducts) {
        for (OrderProduct orderProduct : orderProducts) {
            Product product = productService.getProduct(orderProduct.getProductId());
            orderProduct.assignOrderInfo(order.getId(), product.getPrice());
            orderProductRepository.save(orderProduct);
        }
    }
}
