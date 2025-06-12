package kr.hhplus.be.server.interfaces.web.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.application.order.OrderService;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.interfaces.web.order.dto.request.OrderRequest;
import kr.hhplus.be.server.interfaces.web.order.dto.request.OrderProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void 주문을_생성한다() throws Exception {
        // given
        Long orderId = 12345L;
        Long userId = 1L;
        Long productId = 1L;
        Long quantity = 2L;

        OrderProductRequest orderProductRequest = OrderProductRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();

        OrderRequest request = OrderRequest.builder()
                .userId(userId)
                .orderProducts(List.of(orderProductRequest))
                .build();

        Order savedOrder = Order.builder()
                .id(orderId)
                .userId(userId)
                .build();

        given(orderService.createOrder(any(), any())).willReturn(savedOrder);

        // when & then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.message").value("주문 생성 성공"));
    }
}
