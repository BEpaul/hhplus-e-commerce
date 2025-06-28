package kr.hhplus.be.server.infrastructure.config.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedissonClient redissonClient;

    /**
     * 분산락을 사용하여 작업을 실행
     * @param lockKey 락 키
     * @param waitTime 락 대기 시간 (초)
     * @param leaseTime 락 유지 시간 (초)
     * @param task 실행할 작업
     * @return 작업 결과
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, Supplier<T> task) {
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            log.debug("락 획득 시도 - 키: {}", lockKey);
            
            if (lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)) {
                try {
                    log.debug("락 획득 성공 - 키: {}", lockKey);
                    return task.get();
                } finally {
                    lock.unlock();
                    log.debug("락 해제 완료 - 키: {}", lockKey);
                }
            } else {
                log.warn("락 획득 실패 - 키: {}", lockKey);
                throw new RuntimeException("락 획득 실패: " + lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("락 획득 중 인터럽트 발생 - 키: {}", lockKey, e);
            throw new RuntimeException("락 획득 중 인터럽트 발생: " + lockKey, e);
        }
    }

    /**
     * 분산락을 사용하여 작업을 실행 (반환값 없음)
     * @param lockKey 락 키
     * @param waitTime 락 대기 시간 (초)
     * @param leaseTime 락 유지 시간 (초)
     * @param task 실행할 작업
     */
    public void executeWithLock(String lockKey, long waitTime, long leaseTime, Runnable task) {
        executeWithLock(lockKey, waitTime, leaseTime, () -> {
            task.run();
            return null;
        });
    }

    /**
     * 주문 생성용 분산락
     * @param userId 사용자 ID
     * @param task 실행할 작업
     * @return 작업 결과
     */
    public <T> T executeOrderLock(Long userId, Supplier<T> task) {
        String lockKey = "order:user:" + userId;
        return executeWithLock(lockKey, 10, 30, task);
    }

    /**
     * 결제 처리용 분산락
     * @param paymentId 결제 ID
     * @param task 실행할 작업
     * @return 작업 결과
     */
    public <T> T executePaymentLock(Long paymentId, Supplier<T> task) {
        String lockKey = "payment:" + paymentId;
        return executeWithLock(lockKey, 10, 30, task);
    }

    /**
     * 포인트 차감용 분산락
     * @param userId 사용자 ID
     * @param task 실행할 작업
     * @return 작업 결과
     */
    public <T> T executePointLock(Long userId, Supplier<T> task) {
        String lockKey = "point:user:" + userId;
        return executeWithLock(lockKey, 10, 30, task);
    }

    /**
     * 상품 재고 차감용 분산락
     * @param productId 상품 ID
     * @param task 실행할 작업
     * @return 작업 결과
     */
    public <T> T executeProductStockLock(Long productId, Supplier<T> task) {
        String lockKey = "product:stock:" + productId;
        return executeWithLock(lockKey, 10, 30, task);
    }
} 