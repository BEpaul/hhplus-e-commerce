package kr.hhplus.be.server.interfaces.web.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.persistence.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    private Product savedProduct;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        productRepository.deleteAllInBatch();

        // 테스트 데이터 초기화
        Product product = Product.builder()
                .name("테스트 상품")
                .price(10000L)
                .stock(50L)
                .salesCount(0L)
                .description("테스트 상품의 상세 설명입니다.")
                .build();
        savedProduct = productRepository.save(product);
    }

    @Test
    void 상품_단건을_조회한다() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/products/{productId}", savedProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(savedProduct.getId()))
                .andExpect(jsonPath("$.data.name").value("테스트 상품"))
                .andExpect(jsonPath("$.data.price").value(10000))
                .andExpect(jsonPath("$.data.stock").value(50))
                .andExpect(jsonPath("$.data.description").value("테스트 상품의 상세 설명입니다."))
                .andExpect(jsonPath("$.message").value("상품 조회 성공"));
    }

    @Test
    void 상품_목록을_조회한다() throws Exception {
        // given
        Product product2 = Product.builder()
                .name("테스트 상품 2")
                .price(20000L)
                .stock(30L)
                .salesCount(0L)
                .description("테스트 상품 2의 상세 설명입니다.")
                .build();
        productRepository.save(product2);

        // when & then
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("테스트 상품"))
                .andExpect(jsonPath("$.data[0].price").value(10000))
                .andExpect(jsonPath("$.data[0].stock").value(50))
                .andExpect(jsonPath("$.data[0].description").value("테스트 상품의 상세 설명입니다."))
                .andExpect(jsonPath("$.data[1].name").value("테스트 상품 2"))
                .andExpect(jsonPath("$.data[1].price").value(20000))
                .andExpect(jsonPath("$.data[1].stock").value(30))
                .andExpect(jsonPath("$.data[1].description").value("테스트 상품 2의 상세 설명입니다."))
                .andExpect(jsonPath("$.message").value("상품 목록 조회 성공"));
    }
} 