package kr.hhplus.be.server.interfaces.web.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductRankingInfo {

    private Long productId;
    private Long salesCount;
    private Long ranking;
}