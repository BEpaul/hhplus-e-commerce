package kr.hhplus.be.server.domain.point;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.config.BaseTimeEntity;
import kr.hhplus.be.server.common.exception.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static kr.hhplus.be.server.common.exception.ErrorCode.*;


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
            throw new ApiException(NEGATIVE_CHARGE_POINT);
        }

        if (this.volume + amount > 3_000_000) {
            throw new ApiException(EXCEEDS_MAXIMUM_POINT);
        }

        this.volume += amount;
    }

    public void addChargePointHistory(Long amount) {
        PointHistory pointHistory = PointHistory.create(this, amount, TransactionType.CHARGE);
        this.pointHistories.add(pointHistory);
    }

    public void use(Long amount) {
        if (amount <= 0) {
            throw new ApiException(NEGATIVE_USE_POINT);
        }

        if (this.volume < amount) {
            throw new ApiException(NOT_ENOUGH_POINT);
        }

        this.volume -= amount;
    }

    public void addUsePointHistory(Long amount) {
        PointHistory pointHistory = PointHistory.create(this, amount, TransactionType.USE);
        this.pointHistories.add(pointHistory);
    }
}
