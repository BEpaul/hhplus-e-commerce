package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.infrastructure.config.redis.DistributedLockService;
import kr.hhplus.be.server.infrastructure.persistence.point.PointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static kr.hhplus.be.server.common.exception.ErrorCode.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;
    private final DistributedLockService distributedLockService;

    /**
     * 포인트 충전 (분산락 적용)
     */
    @Transactional
    public Point chargePoint(Long userId, Long chargeAmount) {
        return distributedLockService.executePointLock(userId, () -> {
            log.info("포인트 충전 시작 - 사용자 ID: {}, 충전 금액: {}", userId, chargeAmount);
            
            Point point = findPointByUserId(userId);
            point.charge(chargeAmount);
            point.addChargePointHistory(chargeAmount);
            Point savedPoint = pointRepository.save(point);
            
            log.info("포인트 충전 완료 - 사용자 ID: {}, 충전 후 잔액: {}", userId, savedPoint.getVolume());
            return savedPoint;
        });
    }

    @Transactional(readOnly = true)
    public Point getPoint(Long userId) {
        return findPointByUserId(userId);
    }

    /**
     * 포인트 사용 (분산락 적용)
     */
    @Transactional
    public Point usePoint(Long userId, Long useAmount) {
        return distributedLockService.executePointLock(userId, () -> {
            log.info("포인트 사용 시작 - 사용자 ID: {}, 사용 금액: {}", userId, useAmount);
            
            Point point = findPointByUserId(userId);
            point.use(useAmount);
            point.addUsePointHistory(useAmount);
            Point savedPoint = pointRepository.save(point);
            
            log.info("포인트 사용 완료 - 사용자 ID: {}, 사용 후 잔액: {}", userId, savedPoint.getVolume());
            return savedPoint;
        });
    }

    // 중복 제거를 위한 private 메서드
    private Point findPointByUserId(Long userId) {
        return pointRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(USER_NOT_FOUND));
    }
}
