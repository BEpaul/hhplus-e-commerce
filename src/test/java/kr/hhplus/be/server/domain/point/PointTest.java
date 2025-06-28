package kr.hhplus.be.server.domain.point;

import kr.hhplus.be.server.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static kr.hhplus.be.server.common.exception.ErrorCode.*;

public class PointTest {

    @Test
    void 포인트_충전에_성공한다() {
        // given
        Long userId = 1L;
        Long initialPoint = 10000L;
        Long chargeAmount = 50000L;
        Point point = Point.create(userId, initialPoint);

        // when
        point.charge(chargeAmount);

        // then
        assertThat(point.getVolume()).isEqualTo(initialPoint + chargeAmount);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1000, -1, 0})
    void 음수_또는_0포인트를_충전하면_예외가_발생한다(Long chargeAmount) {
        // given
        Long userId = 1L;
        Long initialPoint = 10000L;
        Point point = Point.create(userId, initialPoint);

        // when & then
        assertThatThrownBy(() -> point.charge(chargeAmount))
                .isInstanceOf(ApiException.class)
                .hasMessage(NEGATIVE_CHARGE_POINT.getMessage());
    }

    @Test
    void 포인트_충전_후_300만_포인트가_넘으면_예외가_발생한다() {
         // given
        Long userId = 1L;
        Long initialPoint = 2500000L;
        Long chargeAmount = 600000L; // 충전 후 총액이 3100000이 되어 예외 발생

        Point point = Point.create(userId, initialPoint);

        // when & then
        assertThatThrownBy(() -> point.charge(chargeAmount))
                .isInstanceOf(ApiException.class)
                .hasMessage(EXCEEDS_MAXIMUM_POINT.getMessage());
    }

    @Test
    void 포인트_사용에_성공한다() {
        // given
        Long userId = 1L;
        Long initialPoint = 100000L;
        Long useAmount = 30000L;
        Point point = Point.create(userId, initialPoint);

        // when
        point.use(useAmount);

        // then
        assertThat(point.getVolume()).isEqualTo(initialPoint - useAmount);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1000, -1, 0})
    void 음수_또는_0포인트를_사용하면_예외가_발생한다(Long useAmount) {
        // given
        Long userId = 1L;
        Long initialPoint = 10000L;
        Point point = Point.create(userId, initialPoint);

        // when & then
        assertThatThrownBy(() -> point.use(useAmount))
                .isInstanceOf(ApiException.class)
                .hasMessage(NEGATIVE_USE_POINT.getMessage());
    }

    @Test
    void 보유_포인트보다_많은_포인트를_사용하면_예외가_발생한다() {
        // given
        Long userId = 1L;
        Long initialPoint = 50000L;
        Long useAmount = 70000L; // 보유 포인트보다 많은 금액
        Point point = Point.create(userId, initialPoint);

        // when & then
        assertThatThrownBy(() -> point.use(useAmount))
                .isInstanceOf(ApiException.class)
                .hasMessage(NOT_ENOUGH_POINT.getMessage());
    }

    @Test
    void 보유_포인트와_정확히_같은_금액을_사용할_수_있다() {
        // given
        Long userId = 1L;
        Long initialPoint = 50000L;
        Long useAmount = 50000L; // 보유 포인트와 동일한 금액
        Point point = Point.create(userId, initialPoint);

        // when
        point.use(useAmount);

        // then
        assertThat(point.getVolume()).isEqualTo(0L);
    }
}
