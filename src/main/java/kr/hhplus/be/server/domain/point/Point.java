package kr.hhplus.be.server.domain.point;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.BaseTimeEntity;
import kr.hhplus.be.server.common.exception.ExceedsMaximumPointException;
import kr.hhplus.be.server.common.exception.NegativeChargePointException;
import kr.hhplus.be.server.common.exception.NegativeUsePointException;
import kr.hhplus.be.server.common.exception.NotEnoughPointException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_id")
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long volume;

    @OneToMany(mappedBy = "point", cascade = CascadeType.ALL)
    private List<PointHistory> pointHistories = new ArrayList<>();

    @Builder
    public Point(Long userId, Long volume) {
        this.userId = userId;
        this.volume = volume;
    }

    public static Point create(Long userId, Long volume) {
        return Point.builder()
                .userId(userId)
                .volume(volume)
                .build();
    }

    public void charge(Long amount) {
        if (amount <= 0) {
            throw new NegativeChargePointException("충전 금액은 0보다 커야 합니다.");
        }

        if (this.volume + amount > 3_000_000) {
            throw new ExceedsMaximumPointException("충전 후 포인트가 300만을 초과할 수 없습니다.");
        }

        this.volume += amount;
    }

    public void addChargePointHistory(Long amount) {
        PointHistory pointHistory = PointHistory.create(this, amount, TransactionType.CHARGE);
        this.pointHistories.add(pointHistory);
    }

    public void use(Long amount) {
        if (amount <= 0) {
            throw new NegativeUsePointException("사용 금액은 0보다 커야 합니다.");
        }

        if (this.volume < amount) {
            throw new NotEnoughPointException("포인트가 부족합니다.");
        }

        this.volume -= amount;
    }

    public void addUsePointHistory(Long amount) {
        PointHistory pointHistory = PointHistory.create(this, amount, TransactionType.USE);
        this.pointHistories.add(pointHistory);
    }
}
