package kr.hhplus.be.server.interfaces.web.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.application.order.OrderService;
import kr.hhplus.be.server.common.response.ApiResponse;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.interfaces.web.order.dto.request.OrderRequest;
import kr.hhplus.be.server.interfaces.web.order.dto.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "주문", description = "주문 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성", description = "사용자가 상품을 주문합니다.")
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        Order savedOrder = orderService.placeOrder(
                request.toOrder(),
                request.toOrderProducts()
        );

        return ApiResponse.success(
                OrderResponse.from(savedOrder.getId()),
                "주문 생성 성공"
        );
    }
}
