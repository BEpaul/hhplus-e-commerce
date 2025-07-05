package kr.hhplus.be.server.interfaces.web.bestseller.dto.response;

import kr.hhplus.be.server.domain.bestseller.BestSeller;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BestSellerResponse {
    private Long productId;
    private String name;
    private Long price;
    private Long ranking;
    private Long salesCount;

    public static BestSellerResponse from(BestSeller bestSeller) {
        return BestSellerResponse.builder()
                .productId(bestSeller.getProductId())
                .name(bestSeller.getName())
                .ranking(bestSeller.getRanking())
                .build();
    }
}
