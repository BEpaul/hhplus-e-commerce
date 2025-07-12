package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderProduct;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.OrderStatus;
import kr.hhplus.be.server.domain.order.event.OrderEventPublisher;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.external.orderinfo.DataPlatform;
import kr.hhplus.be.server.infrastructure.persistence.point.PointRepository;
import kr.hhplus.be.server.infrastructure.persistence.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PointRepository pointRepository;

    @MockitoBean
    private DataPlatform dataPlatform;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    private Long userId = 1L;
    private Long productId;
    private Product product;
    private Point point;

    @BeforeEach
    @Transactional
    void setUp() {
        // 기존 데이터 정리
        orderRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        pointRepository.deleteAllInBatch();
        
        // 상품 생성 - 재고 10개
        product = Product.builder()
                .name("동시성 테스트 상품")
                .price(10000L)
                .stock(10L)
                .salesCount(0L)
                .description("동시성 테스트용 상품")
                .build();
        productId = productRepository.save(product).getId();

        // 포인트 생성 - 충분한 포인트
        point = Point.builder()
                .userId(userId)
                .volume(1000000L)
                .build();
        pointRepository.save(point);
    }

    @Test
    void 동시에_10개의_주문이_들어왔을_때_재고가_올바르게_차감된다() throws InterruptedException {
        // given
        int threadCount = 10;
        int quantityPerOrder = 1; // 각 주문당 1개씩 주문
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Order>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // when - 10개의 주문을 동시에 실행
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Order> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Order order = Order.builder()
                            .userId(userId)
                            .build();

                    OrderProduct orderProduct = OrderProduct.builder()
                            .productId(productId)
                            .quantity((long) quantityPerOrder)
                            .build();

                    Order savedOrder = orderService.placeOrder(order, List.of(orderProduct));
                    successCount.incrementAndGet();
                    return savedOrder;
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    throw new RuntimeException(e);
                }
            }, executorService);
            futures.add(future);
        }

        // 모든 주문 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();

        // then
        // 1. 성공한 주문 수 확인 (재고가 10개이므로 10개 주문 모두 성공해야 함)
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failureCount.get()).isEqualTo(0);

        // 2. 실제 저장된 주문 수 확인
        List<Order> savedOrders = orderRepository.findAll();
        assertThat(savedOrders).hasSize(10);

        // 3. 상품 재고가 0이 되었는지 확인
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(0L);

        // 4. 모든 주문이 완료 상태인지 확인
        long completedOrderCount = savedOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .count();
        assertThat(completedOrderCount).isEqualTo(10);
    }

    @Test
    void 재고보다_많은_주문이_동시에_들어왔을_때_일부만_성공한다() throws InterruptedException {
        // given
        int threadCount = 15; // 재고 10개보다 많은 15개 주문
        int quantityPerOrder = 1;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Order>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // when - 15개의 주문을 동시에 실행
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Order> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Order order = Order.builder()
                            .userId(userId)
                            .build();

                    OrderProduct orderProduct = OrderProduct.builder()
                            .productId(productId)
                            .quantity((long) quantityPerOrder)
                            .build();

                    Order savedOrder = orderService.placeOrder(order, List.of(orderProduct));
                    successCount.incrementAndGet();
                    return savedOrder;
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    return null;
                }
            }, executorService);
            futures.add(future);
        }

        // 모든 주문 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();

        // then
        // 1. 성공한 주문은 10개, 실패한 주문은 5개
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failureCount.get()).isEqualTo(5);

        // 2. 실제 저장된 주문 수 확인
        List<Order> savedOrders = orderRepository.findAll();
        assertThat(savedOrders).hasSize(10);

        // 3. 상품 재고가 0이 되었는지 확인
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(0L);

        // 4. 모든 저장된 주문이 완료 상태인지 확인
        long completedOrderCount = savedOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .count();
        assertThat(completedOrderCount).isEqualTo(10);
    }

    @Test
    void 각_주문당_2개씩_주문할_때_동시성_처리가_올바르게_된다() throws InterruptedException {
        // given
        int threadCount = 5; // 5개 주문
        int quantityPerOrder = 2; // 각 주문당 2개씩
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Order>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // when - 5개의 주문을 동시에 실행 (각각 2개씩 = 총 10개)
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Order> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Order order = Order.builder()
                            .userId(userId)
                            .build();

                    OrderProduct orderProduct = OrderProduct.builder()
                            .productId(productId)
                            .quantity((long) quantityPerOrder)
                            .build();

                    Order savedOrder = orderService.placeOrder(order, List.of(orderProduct));
                    successCount.incrementAndGet();
                    return savedOrder;
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    return null;
                }
            }, executorService);
            futures.add(future);
        }

        // 모든 주문 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();

        // then
        // 1. 성공한 주문은 5개 (재고 10개 / 주문당 2개 = 5개 주문 가능)
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failureCount.get()).isEqualTo(0);

        // 2. 실제 저장된 주문 수 확인
        List<Order> savedOrders = orderRepository.findAll();
        assertThat(savedOrders).hasSize(5);

        // 3. 상품 재고가 0이 되었는지 확인
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(0L);

        // 4. 모든 저장된 주문이 완료 상태인지 확인
        long completedOrderCount = savedOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .count();
        assertThat(completedOrderCount).isEqualTo(5);
    }
}
