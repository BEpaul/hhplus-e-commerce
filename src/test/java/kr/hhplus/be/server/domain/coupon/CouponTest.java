package kr.hhplus.be.server.domain.coupon;

import kr.hhplus.be.server.common.exception.OutOfStockCouponException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {

    private Coupon amountCoupon;

    @BeforeEach
    void setUp() {
        amountCoupon = Coupon.builder()
                .id(1L)
                .discountValue(3000L)
                .discountType(DiscountType.AMOUNT)
                .title("3000원 할인 쿠폰")
                .stock(10L)
                .build();
    }

    @Test
    void 정액_쿠폰이_정상적으로_적용된다() {
        // given
        Long productPrice = 10000L;
        Long expectedPrice = 7000L;

        // when
        Long discountedPrice = amountCoupon.apply(productPrice);

        // then
        assertThat(discountedPrice).isEqualTo(expectedPrice);
    }

    @Test
    void 정액_쿠폰의_할인금액이_상품_가격보다_크면_0원을_반환한다() {
        // given
        Long productPrice = 2000L;
        Long expectedPrice = 0L;

        // when
        Long discountedPrice = amountCoupon.apply(productPrice);

        // then
        assertThat(discountedPrice).isEqualTo(expectedPrice);
    }

    @ParameterizedTest
    @CsvSource({
            "10000, 20, 8000",
            "10000, 0, 10000",
            "10000, 100, 0"
    })
    void 정률_쿠폰이_정상적으로_적용된다(Long productPrice, Long discountRate, Long expectedPrice) {
        // given
        Coupon coupon = Coupon.builder()
                .id(3L)
                .discountValue(discountRate)
                .discountType(DiscountType.PERCENT)
                .title(discountRate + "% 할인 쿠폰")
                .stock(10L)
                .build();

        // when
        Long discountedPrice = coupon.apply(productPrice);

        // then
        assertThat(discountedPrice).isEqualTo(expectedPrice);
    }
}
