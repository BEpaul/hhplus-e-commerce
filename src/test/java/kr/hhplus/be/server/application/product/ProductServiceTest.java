package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.persistence.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Map;
import java.util.Optional;

import static kr.hhplus.be.server.common.exception.ErrorCode.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    private Product testProduct;
    private Map<Long, Long> productSalesMap;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(1L)
                .name("테스트 상품")
                .price(10000L)
                .stock(100L)
                .salesCount(0L)
                .description("테스트 상품 설명")
                .build();

        productSalesMap = Map.of(1L, 10L, 2L, 5L, 3L, 15L);
    }

    @Test
    void 상품ID로_단일_상품을_조회한다() {
        // given
        Long productId = 1L;
        given(productRepository.findById(productId)).willReturn(Optional.of(testProduct));

        // when
        Product findProduct = productService.getProduct(productId);

        // then
        assertThat(findProduct.getId()).isEqualTo(productId);
        assertThat(findProduct.getName()).isEqualTo("테스트 상품");
        assertThat(findProduct.getPrice()).isEqualTo(10000L);
        assertThat(findProduct.getStock()).isEqualTo(100L);
        assertThat(findProduct.getDescription()).isEqualTo("테스트 상품 설명");
    }

    @Test
    void 존재하지_않는_상품_조회_시_예외가_발생한다() {
        // given
        Long productId = 999L;
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(productId))
                .isInstanceOf(ApiException.class)
                .hasMessage(PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    void 상품_목록을_조회한다() {
        // given
        List<Product> products = List.of(
                Product.builder()
                        .name("상품 1")
                        .price(10000L)
                        .stock(50L)
                        .salesCount(0L)
                        .description("상품 1 설명")
                        .build(),
                Product.builder()
                        .name("상품 2")
                        .price(20000L)
                        .stock(30L)
                        .salesCount(0L)
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

    @Test
    void 여러_상품_판매량_업데이트에_성공한다() {
        // when
        productService.updateDailySalesCount(productSalesMap);

        // then
        verify(productRepository, times(3)).updateSalesCount(anyLong(), anyLong());
        verify(productRepository).updateSalesCount(1L, 10L);
        verify(productRepository).updateSalesCount(2L, 5L);
        verify(productRepository).updateSalesCount(3L, 15L);
    }

    @Test
    void 비어있는_맵에_대한_상품_업데이트는_스킵한다() {
        // given
        Map<Long, Long> emptyMap = Map.of();

        // when
        productService.updateDailySalesCount(emptyMap);

        // then
        verify(productRepository, never()).updateSalesCount(anyLong(), anyLong());
    }

    @Test
    void 판매량이_음수인_상품_데일리_정보는_스킵한다() {
        // given
        Map<Long, Long> salesMap = Map.of(1L, 0L, 2L, -5L, 3L, 10L);

        // when
        productService.updateDailySalesCount(salesMap);

        // then
        verify(productRepository, times(1)).updateSalesCount(eq(3L), eq(10L));
        verify(productRepository, never()).updateSalesCount(eq(1L), anyLong());
        verify(productRepository, never()).updateSalesCount(eq(2L), anyLong());
    }

    @Test
    void 단일상품_판매량_업데이트에_성공한다() {
        // when
        productService.updateProductSalesCount(1L, 10L);

        // then
        verify(productRepository, times(1)).updateSalesCount(1L, 10L);
    }

    @Test
    void 판매량이_음수인_단일_상품_정보는_스킵한다() {
        // when
        productService.updateProductSalesCount(1L, 0L);
        productService.updateProductSalesCount(2L, -5L);

        // then
        verify(productRepository, never()).updateSalesCount(anyLong(), anyLong());
    }
}