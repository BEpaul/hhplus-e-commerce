package kr.hhplus.be.server.interfaces.web.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import kr.hhplus.be.server.common.response.ApiResponse;
import kr.hhplus.be.server.interfaces.web.product.dto.response.ProductResponse;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.application.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "상품", description = "상품 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 단건 조회
     */
    @Operation(summary = "상품 단건 조회", description = "상품 ID로 단일 상품을 조회합니다.")
    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable @Min(1) Long productId) {
        Product product = productService.getProduct(productId);

        return ApiResponse.success(ProductResponse.from(product), "상품 조회 성공");
    }

    /**
     * 상품 목록 조회
     */
    @Operation(summary = "상품 목록 조회", description = "모든 상품의 목록을 조회합니다.")
    @GetMapping()
    public ApiResponse<List<ProductResponse>> getProducts() {
        List<Product> products = productService.getProducts();

        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::from)
                .toList();

        return ApiResponse.success(responses, "상품 목록 조회 성공");
    }
}
