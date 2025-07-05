package kr.hhplus.be.server.interfaces.web.bestseller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.application.bestseller.BestSellerRankingService;
import kr.hhplus.be.server.application.bestseller.BestSellerService;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.common.response.ApiResponse;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.interfaces.web.bestseller.dto.response.BestSellerResponse;
import kr.hhplus.be.server.interfaces.web.product.dto.response.ProductRankingInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "베스트셀러", description = "베스트셀러 관련 API")
@RestController
@RequestMapping("/api/v1/bestsellers")
@RequiredArgsConstructor
public class BestSellerController {

    private final BestSellerService bestSellerService;
    private final BestSellerRankingService bestSellerRankingService;
    private final ProductService productService;

    /**
     * 상위 상품 목록 조회 (Redis Sorted Set 기반)
     */
    @Operation(summary = "상위 상품 목록 조회", description = "실시간 판매량 TOP5 상품 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<BestSellerResponse>> getTopProducts() {
        List<ProductRankingInfo> topProducts = bestSellerRankingService.getTopProductsWithRanking();
        
        if (topProducts.isEmpty()) {
            return ApiResponse.success(List.of(), "베스트셀러 데이터가 없습니다");
        }
        
        List<BestSellerResponse> responses = topProducts.stream()
                .map(rankingInfo -> {
                    Product product = productService.getProduct(rankingInfo.getProductId());
                    return BestSellerResponse.builder()
                            .name(product.getName())
                            .productId(product.getId())
                            .price(product.getPrice())
                            .ranking(rankingInfo.getRanking())  // Redis reverseRank로 계산된 실제 랭킹
                            .salesCount(rankingInfo.getSalesCount())
                            .build();
                })
                .collect(Collectors.toList());

        return ApiResponse.success(responses, "상위 상품 목록 조회 성공");
    }
} 