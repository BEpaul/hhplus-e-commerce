package kr.hhplus.be.server.interfaces.web.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.coupon.*;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.order.event.OrderEventPublisher;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.external.orderinfo.DataPlatform;
import kr.hhplus.be.server.infrastructure.persistence.point.PointRepository;
import kr.hhplus.be.server.infrastructure.persistence.product.ProductRepository;
import kr.hhplus.be.server.interfaces.web.order.dto.request.OrderProductRequest;
import kr.hhplus.be.server.interfaces.web.order.dto.request.OrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @MockitoBean
    private DataPlatform dataPlatform;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    private Long userId = 1L;
    private Long productId;
    private Long userCouponId;
    private Product product;
    private Point point;
    private Coupon coupon;
    private UserCoupon userCoupon;

    @BeforeEach
    void setUp() {
        // 상품 생성
        product = Product.builder()
                .name("테스트 상품")
                .price(10000L)
                .stock(10L)
                .salesCount(0L)
                .description("테스트 상품 설명")
                .build();
        productId = productRepository.save(product).getId();

        // 포인트 생성
        point = Point.builder()
                .userId(userId)
                .volume(50000L)
                .build();
        pointRepository.save(point);

        // 쿠폰 생성
        coupon = Coupon.builder()
                .title("테스트 쿠폰")
                .discountType(DiscountType.AMOUNT)
                .discountValue(1000L)
                .stock(10L)
                .build();
        couponRepository.save(coupon);

        // 사용자 쿠폰 생성
        userCoupon = UserCoupon.builder()
                .userId(userId)
                .couponId(coupon.getId())
                .isUsed(false)
                .expiredAt(LocalDateTime.now().plusDays(30))
                .build();
        userCouponId = userCouponRepository.save(userCoupon).getId();
    }

    @Test
    void 주문을_생성한다() throws Exception {
        // given
        OrderRequest request = OrderRequest.builder()
                .userId(userId)
                .userCouponId(userCouponId)
                .orderProducts(List.of(
                        OrderProductRequest.builder()
                                .productId(productId)
                                .quantity(2L)
                                .build()
                ))
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").exists())
                .andExpect(jsonPath("$.message").value("주문 생성 성공"));

        // 주문이 실제로 저장되었는지 확인
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getUserId()).isEqualTo(userId);
        assertThat(orders.get(0).getUserCouponId()).isEqualTo(userCouponId);
    }
}
