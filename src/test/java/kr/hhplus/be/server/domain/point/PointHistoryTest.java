package kr.hhplus.be.server.domain.point;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PointHistoryTest {

    @Test
    void 포인트_히스토리_엔티티를_성공적으로_생성한다() {
        // given
        Point point = Point.builder()
                .userId(1L)
                .volume(10000L)
                .build();
        Long amount = 5000L;
        TransactionType transactionType = TransactionType.CHARGE;

        // when
        PointHistory pointHistory = PointHistory.builder()
                .point(point)
                .amount(amount)
                .transactionType(transactionType)
                .build();

        // then
        assertThat(pointHistory.getPoint()).isEqualTo(point);
        assertThat(pointHistory.getAmount()).isEqualTo(amount);
        assertThat(pointHistory.getTransactionType()).isEqualTo(transactionType);
    }

    @Test
    void 포인트_충전_히스토리를_생성한다() {
        // given
        Point point = Point.builder()
                .userId(1L)
                .volume(10000L)
                .build();
        Long amount = 5000L;

        // when
        PointHistory pointHistory = PointHistory.create(point, amount, TransactionType.CHARGE);

        // then
        assertThat(pointHistory.getPoint()).isEqualTo(point);
        assertThat(pointHistory.getAmount()).isEqualTo(amount);
        assertThat(pointHistory.getTransactionType()).isEqualTo(TransactionType.CHARGE);
    }

    @Test
    void 포인트_사용_히스토리를_생성한다() {
        // given
        Point point = Point.builder()
                .userId(1L)
                .volume(10000L)
                .build();
        Long amount = 3000L;

        // when
        PointHistory pointHistory = PointHistory.create(point, amount, TransactionType.USE);

        // then
        assertThat(pointHistory.getPoint()).isEqualTo(point);
        assertThat(pointHistory.getAmount()).isEqualTo(amount);
        assertThat(pointHistory.getTransactionType()).isEqualTo(TransactionType.USE);
    }

    @Test
    void 포인트_히스토리_생성_시_ID가_null이면_자동_생성된다() {
        // given
        Point point = Point.builder()
                .userId(1L)
                .volume(10000L)
                .build();

        // when
        PointHistory pointHistory = PointHistory.create(point, 5000L, TransactionType.CHARGE);

        // then
        assertThat(pointHistory.getId()).isNull(); // JPA가 저장 시점에 ID를 생성하므로 null
    }

    @Test
    void 포인트_히스토리_생성_시_ID는_자동_생성된다() {
        // given
        Point point = Point.builder()
                .userId(1L)
                .volume(10000L)
                .build();

        // when
        PointHistory pointHistory = PointHistory.create(point, 5000L, TransactionType.CHARGE);

        // then
        assertThat(pointHistory.getId()).isNull(); // JPA가 저장 시점에 ID를 생성하므로 null
    }

    @Test
    void 다양한_금액의_포인트_히스토리를_생성한다() {
        // given
        Point point = Point.builder()
                .userId(1L)
                .volume(10000L)
                .build();
        Long smallAmount = 100L;
        Long largeAmount = 10000L;

        // when
        PointHistory smallHistory = PointHistory.create(point, smallAmount, TransactionType.CHARGE);
        PointHistory largeHistory = PointHistory.create(point, largeAmount, TransactionType.USE);

        // then
        assertThat(smallHistory.getAmount()).isEqualTo(100L);
        assertThat(largeHistory.getAmount()).isEqualTo(10000L);
        assertThat(smallHistory.getTransactionType()).isEqualTo(TransactionType.CHARGE);
        assertThat(largeHistory.getTransactionType()).isEqualTo(TransactionType.USE);
    }
} 