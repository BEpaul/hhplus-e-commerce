package kr.hhplus.be.server.application.point;

import kr.hhplus.be.server.common.exception.NotFoundUserException;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.infrastructure.persistence.point.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;

    @Transactional
    public Point chargePoint(Long userId, Long chargeAmount) {
        Point point = findPointByUserId(userId);
        point.charge(chargeAmount);
        point.addChargePointHistory(chargeAmount);
        return pointRepository.save(point);
    }

    @Transactional(readOnly = true)
    public Point getPoint(Long userId) {
        return findPointByUserId(userId);
    }

    @Transactional
    public Point usePoint(Long userId, Long useAmount) {
        Point point = findPointByUserId(userId);
        point.use(useAmount);
        point.addUsePointHistory(useAmount);
        return pointRepository.save(point);
    }

    // 중복 제거를 위한 private 메서드
    private Point findPointByUserId(Long userId) {
        return pointRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundUserException("사용자를 찾을 수 없습니다."));
    }
}
