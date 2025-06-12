package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.persistence.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;


@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Test
    void 상품ID로_단일_상품을_조회한다() {
        // given
        Long productId = 1L;
        Product product = Product.builder()
                .name("상품 A")
                .price(10000L)
                .stock(50L)
                .description("상품 A 설명")
                .build();
        
        // id 설정
        ReflectionTestUtils.setField(product, "id", productId);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        Product findProduct = productService.getProduct(productId);

        // then
        assertThat(findProduct.getId()).isEqualTo(productId);
        assertThat(findProduct.getName()).isEqualTo("상품 A");
        assertThat(findProduct.getPrice()).isEqualTo(10000L);
        assertThat(findProduct.getStock()).isEqualTo(50L);
        assertThat(findProduct.getDescription()).isEqualTo("상품 A 설명");
    }

    @Test
    void 존재하지_않는_상품_조회_시_예외가_발생한다() {
        // given
        Long productId = 999L;
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(productId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상품이 존재하지 않습니다.");
    }

    @Test
    void 상품_목록을_조회한다() {
        // given
        List<Product> products = List.of(
                Product.builder()
                        .name("상품 1")
                        .price(10000L)
                        .stock(50L)
                        .description("상품 1 설명")
                        .build(),
                Product.builder()
                        .name("상품 2")
                        .price(20000L)
                        .stock(30L)
                        .description("상품 2 설명")
                        .build()
        );

        given(productRepository.findAll()).willReturn(products);

        // when
        List<Product> findProducts = productService.getProducts();

        // then
        assertThat(findProducts).hasSize(2);
        assertThat(findProducts.get(0).getName()).isEqualTo("상품 1");
        assertThat(findProducts.get(1).getName()).isEqualTo("상품 2");
    }
}