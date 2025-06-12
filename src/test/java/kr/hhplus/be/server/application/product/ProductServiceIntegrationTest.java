package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.persistence.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private Product savedProduct;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        Product product = Product.builder()
                .name("테스트 상품")
                .price(10000L)
                .stock(50L)
                .description("테스트 상품의 상세 설명입니다.")
                .build();
        savedProduct = productRepository.save(product);
    }

    @Test
    void 상품_단건을_조회한다() {
        // when
        Product foundProduct = productService.getProduct(savedProduct.getId());

        // then
        assertThat(foundProduct.getId()).isEqualTo(savedProduct.getId());
        assertThat(foundProduct.getName()).isEqualTo("테스트 상품");
        assertThat(foundProduct.getPrice()).isEqualTo(10000L);
        assertThat(foundProduct.getStock()).isEqualTo(50L);
        assertThat(foundProduct.getDescription()).isEqualTo("테스트 상품의 상세 설명입니다.");
    }

    @Test
    void 존재하지_않는_상품을_조회하면_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() -> productService.getProduct(999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 상품_목록을_조회한다() {
        // given
        Product product2 = Product.builder()
                .name("테스트 상품 2")
                .price(20000L)
                .stock(30L)
                .description("테스트 상품 2의 상세 설명입니다.")
                .build();
        productRepository.save(product2);

        // when
        List<Product> products = productService.getProducts();

        // then
        assertThat(products).hasSize(2);
        assertThat(products).extracting("name")
                .containsExactlyInAnyOrder("테스트 상품", "테스트 상품 2");
        assertThat(products).extracting("price")
                .containsExactlyInAnyOrder(10000L, 20000L);
        assertThat(products).extracting("stock")
                .containsExactlyInAnyOrder(50L, 30L);
    }
}
