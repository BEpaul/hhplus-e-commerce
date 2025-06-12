package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.common.exception.NotFoundUserException;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.domain.point.PointHistory;
import kr.hhplus.be.server.domain.point.TransactionType;
import kr.hhplus.be.server.infrastructure.persistence.point.PointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private PointRepository pointRepository;

    @Nested
    class GetPointTest {

        private Point point;

        @BeforeEach
        void setUp() {
            point = Point.create(1L, 10000L);
            pointRepository.save(point);
        }

        @Test
        void 사용자의_포인트를_조회한다() {
            // when
            Point result = pointService.getPoint(1L);

            // then
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getVolume()).isEqualTo(10000L);
        }

        @Test
        void 존재하지_않는_사용자의_포인트를_조회하면_예외가_발생한다() {
            assertThatThrownBy(() -> pointService.getPoint(999L))
                    .isInstanceOf(NotFoundUserException.class);
        }
    }

    @Nested
    class ChargePointTest {

        private Point point;

        @BeforeEach
        void setUp() {
            point = Point.create(1L, 10000L);
            pointRepository.save(point);
        }

        @Test
        void 포인트를_충전한다() {
            // when
            Point result = pointService.chargePoint(1L, 5000L);

            // then
            assertThat(result.getVolume()).isEqualTo(15000L);
            assertThat(result.getPointHistories()).hasSize(1);
            assertThat(result.getPointHistories().get(0))
                .extracting(PointHistory::getAmount, PointHistory::getTransactionType)
                .containsExactly(5000L, TransactionType.CHARGE);
        }

        @Test
        void 존재하지_않는_사용자의_포인트를_충전하면_예외가_발생한다() {
            assertThatThrownBy(() -> pointService.chargePoint(999L, 5000L))
                    .isInstanceOf(NotFoundUserException.class);
        }
    }

    @Nested
    class UsePointTest {

        private Point point;

        @BeforeEach
        void setUp() {
            point = Point.create(1L, 10000L);
            pointRepository.save(point);
        }

        @Test
        void 포인트를_사용한다() {
            // when
            Point result = pointService.usePoint(1L, 5000L);

            // then
            assertThat(result.getVolume()).isEqualTo(5000L);
            assertThat(result.getPointHistories()).hasSize(1);
            assertThat(result.getPointHistories().get(0))
                .extracting(PointHistory::getAmount, PointHistory::getTransactionType)
                .containsExactly(5000L, TransactionType.USE);
        }

        @Test
        void 존재하지_않는_사용자의_포인트를_사용하면_예외가_발생한다() {
            assertThatThrownBy(() -> pointService.usePoint(999L, 5000L))
                    .isInstanceOf(NotFoundUserException.class);
        }
    }
}
