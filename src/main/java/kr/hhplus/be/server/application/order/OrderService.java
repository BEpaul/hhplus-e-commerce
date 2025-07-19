package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.application.bestseller.BestSellerRankingService;
import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.domain.order.event.OrderEventPublisher;
import kr.hhplus.be.server.application.payment.PaymentService;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.order.OrderProductRepository;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.payment.PaymentMethod;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.config.redis.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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
    private final BestSellerRankingService bestSellerRankingService;
    private final OrderEventPublisher orderEventPublisher;

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
     * 11. 베스트셀러 랭킹 업데이트
     */
    @Transactional
    public Order placeOrder(Order order, List<OrderProduct> orderProducts) {
        return distributedLockService.executeOrderLock(order.getUserId(), () -> {
            log.info("주문 생성 시작 - 사용자 ID: {}", order.getUserId());

            validateOrderProducts(orderProducts);
            decreaseProductStocksWithLock(orderProducts);
            
            Map<Long, Product> productMap = getProductMap(orderProducts);

            long totalPrice = calculateTotalPrice(orderProducts, productMap);
            totalPrice = calculateDiscountedPrice(order, totalPrice);
            processCouponUsage(order);

            Order savedOrder = saveOrder(order, totalPrice);
            initiatePayment(savedOrder, totalPrice);
            saveOrderProducts(savedOrder, orderProducts, productMap);

            updateBestSellerRanking(orderProducts);

            List<Product> products = orderProducts.stream()
                    .map(orderProduct -> productMap.get(orderProduct.getProductId()))
                    .toList();
            
            orderEventPublisher.publishOrderCompletedEvent(savedOrder, orderProducts, products);

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
     * 상품 ID 순서로 정렬하여 데드락 방지
     */
    private void decreaseProductStocksWithLock(List<OrderProduct> orderProducts) {
        // 상품 ID 순서로 정렬하여 데드락 방지
        orderProducts.stream()
                .sorted((op1, op2) -> Long.compare(op1.getProductId(), op2.getProductId()))
                .forEach(orderProduct -> {
                    distributedLockService.executeProductStockLock(orderProduct.getProductId(), () -> {
                        Product product = productService.getProductWithPessimisticLock(orderProduct.getProductId());
                        product.decreaseStock(orderProduct.getQuantity());
                        log.info("상품 재고 감소 - 상품 ID: {}, 수량: {}", orderProduct.getProductId(), orderProduct.getQuantity());
                        return null;
                    });
                });
    }

    private Map<Long, Product> getProductMap(List<OrderProduct> orderProducts) {
        List<Long> productIds = orderProducts.stream()
                .map(OrderProduct::getProductId)
                .distinct()
                .toList();
        Map<Long, Product> productMap = productService.getProductMapByIds(productIds);
        return productMap;
    }

    private long calculateTotalPrice(List<OrderProduct> orderProducts, Map<Long, Product> productMap) {
        return orderProducts.stream()
                .mapToLong(it -> {
                    Product product = productMap.get(it.getProductId());
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
     * 결제 처리 시작
     */
    private void initiatePayment(Order order, long totalPrice) {
        try {
            String idempotencyKey = generateIdempotencyKey(order.getId());

            paymentService.processPayment(
                    order.getId(),
                    order.getUserId(),
                    totalPrice,
                    PaymentMethod.POINT,
                    idempotencyKey
            );

            order.success();
            log.info("결제 처리 완료 - 주문 ID: {}", order.getId());
        } catch (Exception e) {
            order.markAsFailed();
            orderRepository.save(order);
            log.error("결제 처리 실패 - 주문 ID: {}", order.getId(), e);
            throw new ApiException(PAYMENT_FAILED);
        }
    }

    private void saveOrderProducts(Order order, List<OrderProduct> orderProducts, Map<Long, Product> productMap) {
        for (OrderProduct orderProduct : orderProducts) {
            Product product = productMap.get(orderProduct.getProductId());
            orderProduct.assignOrderInfo(order.getId(), product.getPrice());
            orderProductRepository.save(orderProduct);
        }
    }

    /**
     * 베스트셀러 랭킹 업데이트
     */
    private void updateBestSellerRanking(List<OrderProduct> orderProducts) {
        try {
            for (OrderProduct orderProduct : orderProducts) {
                bestSellerRankingService.incrementTodaySales(
                        orderProduct.getProductId(),
                        orderProduct.getQuantity()
                );
            }
            log.info("베스트셀러 랭킹 업데이트 완료 - 주문 상품 수: {}", orderProducts.size());
        } catch (Exception e) {
            log.error("베스트셀러 랭킹 업데이트 실패", e);
        }
    }

    /**
     * 멱등성 키 생성
     */
    private String generateIdempotencyKey(Long orderId) {
        return String.format("ORDER_%d_%d", orderId, System.currentTimeMillis());
    }
}
