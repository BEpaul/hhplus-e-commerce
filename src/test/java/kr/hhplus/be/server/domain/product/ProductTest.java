package kr.hhplus.be.server.domain.product;

import kr.hhplus.be.server.common.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static kr.hhplus.be.server.common.exception.ErrorCode.*;

class ProductTest {

    @Test
    void 정상적으로_상품_재고가_차감된다() {
        // given
        Long stock = 10L;
        Long orderQuantity = 5L;
        Long expectedStock = stock - orderQuantity;

        Product product = Product.builder()
                .name("상품 A")
                .price(10000L)
                .stock(stock)
                .salesCount(0L)
                .description("상품 A 설명")
                .build();

        // when
        product.decreaseStock(orderQuantity);

        // then
        assertThat(product.getStock()).isEqualTo(expectedStock);
    }

    @Test
    void 주문_수량보다_상품_재고가_부족할_경우_예외가_발생한다() {
        // given
        Long stock = 5L;
        Long orderQuantity = 10L;

        Product product = Product.builder()
                .name("상품 A")
                .price(10000L)
                .stock(stock)
                .salesCount(0L)
                .description("상품 A 설명")
                .build();

        // when & then
        assertThatThrownBy(() -> product.decreaseStock(orderQuantity))
                .isInstanceOf(ApiException.class)
                .hasMessage(OUT_OF_STOCK_PRODUCT.getMessage());

        assertThat(product.getStock()).isEqualTo(stock);
    }
}
