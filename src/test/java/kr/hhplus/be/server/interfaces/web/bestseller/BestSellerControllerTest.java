package kr.hhplus.be.server.interfaces.web.bestseller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.application.bestseller.BestSellerRankingService;
import kr.hhplus.be.server.application.bestseller.BestSellerService;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.interfaces.web.product.dto.response.ProductRankingInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BestSellerController.class)
public class BestSellerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BestSellerService bestSellerService;

    @MockitoBean
    private BestSellerRankingService bestSellerRankingService;

    @MockitoBean
    private ProductService productService;

    private List<ProductRankingInfo> mockProductRankingInfos;
    private List<Product> mockProducts;

    @BeforeEach
    void setUp() {
        // ProductRankingInfo 모킹 데이터 설정
        mockProductRankingInfos = List.of(
            new ProductRankingInfo(1L, 500L, 1L),
            new ProductRankingInfo(2L, 400L, 2L),
            new ProductRankingInfo(3L, 300L, 3L),
            new ProductRankingInfo(4L, 200L, 4L),
            new ProductRankingInfo(5L, 100L, 5L)
        );

        // Product 모킹 데이터 설정
        mockProducts = List.of(
            Product.builder().id(1L).name("메론킥").price(1000L).stock(100L).build(),
            Product.builder().id(2L).name("엄마손").price(2000L).stock(50L).build(),
            Product.builder().id(3L).name("깡깡깡").price(1500L).stock(80L).build(),
            Product.builder().id(4L).name("스파게티").price(3000L).stock(30L).build(),
            Product.builder().id(5L).name("한우").price(5000L).stock(20L).build()
        );
    }

    @Test
    void 상위_상품_목록_조회에_성공한다() throws Exception {
        // given
        given(bestSellerRankingService.getTopProductsWithRanking()).willReturn(mockProductRankingInfos);
        
        // 각 상품 ID에 대한 Product 모킹 설정
        for (int i = 0; i < mockProducts.size(); i++) {
            given(productService.getProduct(mockProducts.get(i).getId())).willReturn(mockProducts.get(i));
        }

        // when & then
        mockMvc.perform(get("/api/v1/bestsellers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("상위 상품 목록 조회 성공"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(5))
            .andExpect(jsonPath("$.data[0].name").value("메론킥"))
            .andExpect(jsonPath("$.data[0].productId").value(1))
            .andExpect(jsonPath("$.data[0].price").value(1000))
            .andExpect(jsonPath("$.data[0].ranking").value(1))
            .andExpect(jsonPath("$.data[0].salesCount").value(500))
            .andExpect(jsonPath("$.data[1].name").value("엄마손"))
            .andExpect(jsonPath("$.data[1].productId").value(2))
            .andExpect(jsonPath("$.data[1].price").value(2000))
            .andExpect(jsonPath("$.data[1].ranking").value(2))
            .andExpect(jsonPath("$.data[1].salesCount").value(400))
            .andExpect(jsonPath("$.data[2].name").value("깡깡깡"))
            .andExpect(jsonPath("$.data[2].productId").value(3))
            .andExpect(jsonPath("$.data[2].price").value(1500))
            .andExpect(jsonPath("$.data[2].ranking").value(3))
            .andExpect(jsonPath("$.data[2].salesCount").value(300))
            .andExpect(jsonPath("$.data[3].name").value("스파게티"))
            .andExpect(jsonPath("$.data[3].productId").value(4))
            .andExpect(jsonPath("$.data[3].price").value(3000))
            .andExpect(jsonPath("$.data[3].ranking").value(4))
            .andExpect(jsonPath("$.data[3].salesCount").value(200))
            .andExpect(jsonPath("$.data[4].name").value("한우"))
            .andExpect(jsonPath("$.data[4].productId").value(5))
            .andExpect(jsonPath("$.data[4].price").value(5000))
            .andExpect(jsonPath("$.data[4].ranking").value(5))
            .andExpect(jsonPath("$.data[4].salesCount").value(100));
    }
}
