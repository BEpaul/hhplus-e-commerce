package kr.hhplus.be.server.domain.bestseller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BestSellerTest {

    @Test
    void 베스트셀러_엔티티를_성공적으로_생성한다() {
        // given
        String name = "베스트 상품";
        Long productId = 1L;
        Long price = 10000L;
        Long ranking = 1L;
        LocalDateTime topDate = LocalDateTime.now();

        // when
        BestSeller bestSeller = BestSeller.builder()
                .name(name)
                .productId(productId)
                .price(price)
                .ranking(ranking)
                .topDate(topDate)
                .build();

        // then
        assertThat(bestSeller.getName()).isEqualTo(name);
        assertThat(bestSeller.getProductId()).isEqualTo(productId);
        assertThat(bestSeller.getPrice()).isEqualTo(price);
        assertThat(bestSeller.getRanking()).isEqualTo(ranking);
        assertThat(bestSeller.getTopDate()).isEqualTo(topDate);
    }

    @Test
    void 베스트셀러_생성_시_ID가_null이면_자동_생성된다() {
        // given & when
        BestSeller bestSeller = BestSeller.builder()
                .name("베스트 상품")
                .productId(1L)
                .price(10000L)
                .ranking(1L)
                .topDate(LocalDateTime.now())
                .build();

        // then
        assertThat(bestSeller.getId()).isNull(); // JPA가 저장 시점에 ID를 생성하므로 null
    }

    @Test
    void 베스트셀러_생성_시_명시적으로_ID를_설정할_수_있다() {
        // given
        Long bestSellerId = 1L;

        // when
        BestSeller bestSeller = BestSeller.builder()
                .id(bestSellerId)
                .name("베스트 상품")
                .productId(1L)
                .price(10000L)
                .ranking(1L)
                .topDate(LocalDateTime.now())
                .build();

        // then
        assertThat(bestSeller.getId()).isEqualTo(bestSellerId);
    }

    @Test
    void 베스트셀러_순위가_높은_상품을_생성한다() {
        // given
        Long ranking = 1L;

        // when
        BestSeller bestSeller = BestSeller.builder()
                .name("1위 상품")
                .productId(1L)
                .price(10000L)
                .ranking(ranking)
                .topDate(LocalDateTime.now())
                .build();

        // then
        assertThat(bestSeller.getRanking()).isEqualTo(1L);
        assertThat(bestSeller.getName()).isEqualTo("1위 상품");
    }

    @Test
    void 베스트셀러_순위가_낮은_상품을_생성한다() {
        // given
        Long ranking = 10L;

        // when
        BestSeller bestSeller = BestSeller.builder()
                .name("10위 상품")
                .productId(10L)
                .price(5000L)
                .ranking(ranking)
                .topDate(LocalDateTime.now())
                .build();

        // then
        assertThat(bestSeller.getRanking()).isEqualTo(10L);
        assertThat(bestSeller.getName()).isEqualTo("10위 상품");
        assertThat(bestSeller.getPrice()).isEqualTo(5000L);
    }
} 