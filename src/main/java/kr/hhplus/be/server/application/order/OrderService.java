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
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentMethod;
import kr.hhplus.be.server.domain.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final CouponService couponService;
    private final ProductService productService;
    private final PointService pointService;
    private final PaymentService paymentService;

    /**
     * 주문 생성
     */
    @Transactional
    public Order createOrder(Order order, List<OrderProduct> orderProducts) {
        validateOrderProducts(orderProducts);

        long totalPrice = calculateTotalPrice(orderProducts);
        totalPrice = applyCouponIfExists(order, totalPrice);

        Order savedOrder = saveOrder(order, totalPrice);
        processPayment(savedOrder, totalPrice);
        saveOrderProducts(savedOrder, orderProducts);

        return savedOrder;
    }

    private void validateOrderProducts(List<OrderProduct> orderProducts) {
        if (orderProducts == null || orderProducts.isEmpty()) {
            throw new OrderProductEmptyException("주문 상품이 존재하지 않습니다.");
        }
    }

    private long calculateTotalPrice(List<OrderProduct> orderProducts) {
        long totalPrice = 0L;
        for (OrderProduct orderProduct : orderProducts) {
            Product product = productService.getProduct(orderProduct.getProductId());
            product.decreaseStock(orderProduct.getQuantity());
            totalPrice += product.getPrice() * orderProduct.getQuantity();
        }
        return totalPrice;
    }

    private long applyCouponIfExists(Order order, long totalPrice) {
        if (order.getUserCouponId() != null) {
            totalPrice = couponService.calculateDiscountPrice(order.getUserCouponId(), totalPrice);
            couponService.useCoupon(order.getUserCouponId());
            order.applyCoupon();
        }
        return totalPrice;
    }

    private Order saveOrder(Order order, long totalPrice) {
        order.calculateTotalAmount(totalPrice);
        pointService.usePoint(order.getUserId(), totalPrice);
        return orderRepository.save(order);
    }

    private void processPayment(Order order, long totalPrice) {
        boolean result = paymentService.processPayment(
            Payment.create(order.getId(), PaymentMethod.POINT, totalPrice)
        );

        if (!result) {
            throw new FailedPaymentException("결제에 실패했습니다.");
        }

        order.success();
    }

    private void saveOrderProducts(Order order, List<OrderProduct> orderProducts) {
        for (OrderProduct orderProduct : orderProducts) {
            Product product = productService.getProduct(orderProduct.getProductId());
            orderProduct.assignOrderInfo(order.getId(), product.getPrice());
            orderProductRepository.save(orderProduct);
        }
    }
}
