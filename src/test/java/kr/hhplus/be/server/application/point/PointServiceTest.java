package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.common.exception.ExceedsMaximumPointException;
import kr.hhplus.be.server.common.exception.NegativeChargePointException;
import kr.hhplus.be.server.common.exception.NegativeUsePointException;
import kr.hhplus.be.server.common.exception.NotEnoughPointException;
import kr.hhplus.be.server.common.exception.NotFoundUserException;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.infrastructure.persistence.point.PointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private PointRepository pointRepository;

    private Long userId;
    private Point point;

    @BeforeEach
    void setUp() {
        userId = 1L;
        point = Point.create(userId, 1000000L);
    }

    @Test
    void 포인트를_정상적으로_충전한다() {
        // given
        Long chargeAmount = 500000L;
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(point));
        given(pointRepository.save(any(Point.class))).willReturn(point);

        // when
        Point point = pointService.chargePoint(userId, chargeAmount);

        // then
        assertThat(point.getVolume()).isEqualTo(1500000L);
        then(pointRepository).should().findByUserId(userId);
        then(pointRepository).should().save(any(Point.class));
    }

    @Test
    void 음수_금액으로_충전할_수_없다() {
        // given
        Long negativeAmount = -100000L;
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(point));

        // when & then
        assertThatThrownBy(() -> pointService.chargePoint(userId, negativeAmount))
                .isInstanceOf(NegativeChargePointException.class);
    }

    @Test
    void 충전_후_포인트가_300만원을_초과할_수_없다() {
        // given
        Long currentAmount = 2_900_000L;
        Long chargeAmount = 200_000L;
        point = Point.create(userId, currentAmount);
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(point));

        // when & then
        assertThatThrownBy(() -> pointService.chargePoint(userId, chargeAmount))
                .isInstanceOf(ExceedsMaximumPointException.class);
    }

    @Test
    void 사용자의_현재_포인트_잔액을_조회한다() {
        // given
        Long volume = 1000L;
        point = Point.create(userId, volume);
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(point));

        // when
        Point point = pointService.getPoint(userId);

        // then
        assertThat(point.getVolume()).isEqualTo(volume);
        then(pointRepository).should().findByUserId(userId);
    }

    @Test
    void 존재하지_않는_사용자의_포인트_잔액_조회_시_예외가_발생한다() {
        // given
        Long nonExistentUserId = 999L;
        given(pointRepository.findByUserId(nonExistentUserId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointService.getPoint(nonExistentUserId))
                .isInstanceOf(NotFoundUserException.class);
    }

    @Test
    void 포인트를_정상적으로_사용한다() {
        // given
        Long useAmount = 300000L;
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(point));
        given(pointRepository.save(any(Point.class))).willReturn(point);

        // when
        Point result = pointService.usePoint(userId, useAmount);

        // then
        assertThat(result.getVolume()).isEqualTo(700000L);
        then(pointRepository).should().findByUserId(userId);
        then(pointRepository).should().save(any(Point.class));
    }

    @Test
    void 음수_금액으로_포인트를_사용할_수_없다() {
        // given
        Long negativeAmount = -100000L;
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(point));

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, negativeAmount))
                .isInstanceOf(NegativeUsePointException.class);
    }

    @Test
    void 잔액보다_많은_포인트를_사용할_수_없다() {
        // given
        Long useAmount = 1500000L; // 잔액(1000000L)보다 많은 금액
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(point));

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, useAmount))
                .isInstanceOf(NotEnoughPointException.class);
    }

    @Test
    void 존재하지_않는_사용자가_포인트를_사용할_수_없다() {
        // given
        Long nonExistentUserId = 999L;
        Long useAmount = 100000L;
        given(pointRepository.findByUserId(nonExistentUserId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(nonExistentUserId, useAmount))
                .isInstanceOf(NotFoundUserException.class);
    }
}
