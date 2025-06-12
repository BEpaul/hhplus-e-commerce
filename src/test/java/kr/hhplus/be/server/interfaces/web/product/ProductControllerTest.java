package kr.hhplus.be.server.interfaces.web.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.application.product.ProductService;
import kr.hhplus.be.server.domain.product.Product;
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

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @Test
    void 상품_단건을_조회한다() throws Exception {
        // given
        Long productId = 1L;
        Product product = Product.builder()
                .id(1L)
                .name("상품 A")
                .price(10000L)
                .stock(50L)
                .description("상품 A의 상세 설명입니다.")
                .build();
        given(productService.getProduct(productId)).willReturn(product);

        // when & then
        mockMvc.perform(get("/api/v1/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.name").value("상품 A"))
                .andExpect(jsonPath("$.data.price").value(10000))
                .andExpect(jsonPath("$.data.stock").value(50))
                .andExpect(jsonPath("$.data.description").value("상품 A의 상세 설명입니다."))
                .andExpect(jsonPath("$.message").value("상품 조회 성공"));
    }

    @Test
    void 상품_목록을_조회한다() throws Exception {
        // given
        Product product1 = Product.builder()
                .id(1L)
                .name("상품 A")
                .price(10000L)
                .stock(50L)
                .description("상품 A의 상세 설명입니다.")
                .build();

        Product product2 = Product.builder()
                .id(2L)
                .name("상품 B")
                .price(20000L)
                .stock(30L)
                .description("상품 B의 상세 설명입니다.")
                .build();

        List<Product> products = List.of(product1, product2);
        given(productService.getProducts()).willReturn(products);

        // when & then
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("상품 A"))
                .andExpect(jsonPath("$.data[0].price").value(10000))
                .andExpect(jsonPath("$.data[0].stock").value(50))
                .andExpect(jsonPath("$.data[0].description").value("상품 A의 상세 설명입니다."))
                .andExpect(jsonPath("$.data[1].name").value("상품 B"))
                .andExpect(jsonPath("$.data[1].price").value(20000))
                .andExpect(jsonPath("$.data[1].stock").value(30))
                .andExpect(jsonPath("$.data[1].description").value("상품 B의 상세 설명입니다."))
                .andExpect(jsonPath("$.message").value("상품 목록 조회 성공"));
    }
}
