# 분산락 구현: Redisson vs Rettuce 비교 분석

## 개요

주문 및 결제 시스템에서 동시성 문제를 해결하기 위해 분산락(Distributed Lock) 구현이 필요하다. Redis 기반 분산락 구현을 위해 **Redisson**과 **Rettuce**를 비교 분석한 결과를 정리한다.

## 1. Rettuce 분석

### 장점
- **가벼운 의존성**: Spring Boot의 기본 Redis 클라이언트로 별도 의존성 불필요
- **단순한 구조**: 기본적인 Redis 명령어만 사용하여 구현
- **학습 곡선 낮음**: Redis 명령어에 대한 이해만 있으면 구현 가능

### 단점
- **수동 구현 필요**: 분산락의 모든 로직을 직접 구현해야 함
- **복잡한 예외 처리**: 락 해제 실패, 네트워크 장애 등 다양한 예외 상황 처리 필요
- **재시도 로직 부재**: 기본적으로 재시도 메커니즘이 없어 별도 구현 필요
- **락 갱신 기능 없음**: 긴 작업 시간에 대한 락 갱신 기능 구현 필요
- **데드락 위험**: 락 해제 실패 시 시스템 전체에 영향

### Rettuce 기반 분산락 구현 예시

```java
@Component
public class RettuceDistributedLock {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public boolean tryLock(String lockKey, String requestId, long expireTime) {
        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, requestId, Duration.ofMillis(expireTime));
        return Boolean.TRUE.equals(result);
    }
    
    public boolean releaseLock(String lockKey, String requestId) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                       "return redis.call('del', KEYS[1]) " +
                       "else return 0 end";
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList(lockKey),
            requestId
        );
        return Long.valueOf(1).equals(result);
    }
}
```

## 2. Redisson 분석

### 장점
- **완성된 분산락 구현**: RedissonLock 클래스로 즉시 사용 가능
- **자동 락 갱신**: Watchdog 메커니즘으로 긴 작업 시간에도 락 유지
- **다양한 락 타입**: RLock, RReadWriteLock, RSemaphore 등 제공
- **강력한 예외 처리**: 네트워크 장애, Redis 장애 등에 대한 자동 처리
- **재시도 메커니즘**: 기본 재시도 로직 내장
- **락 해제 보장**: try-with-resources 패턴으로 안전한 락 해제

### 단점
- **추가 의존성**: Redisson 라이브러리 의존성 추가 필요
- **학습 곡선**: Redisson API 학습 필요
- **메모리 사용량**: 더 많은 메모리 사용

### Redisson 기반 분산락 구현 예시

```java
@Component
public class RedissonDistributedLock {
    
    private final RedissonClient redissonClient;
    
    public void executeWithLock(String lockKey, Runnable task) {
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 락 획득 (최대 10초 대기, 30초 유지)
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    task.run();
                } finally {
                    lock.unlock();
                }
            } else {
                throw new RuntimeException("락 획득 실패");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 인터럽트 발생", e);
        }
    }
}
```

## 3. 성능 비교

| 항목 | Rettuce | Redisson |
|------|---------|----------|
| 구현 복잡도 | 높음 | 낮음 |
| 메모리 사용량 | 낮음 | 높음 |
| 락 획득 성능 | 빠름 | 빠름 |
| 예외 처리 | 수동 | 자동 |
| 락 갱신 | 수동 구현 | 자동 |
| 재시도 로직 | 수동 구현 | 내장 |

## 4. 결론: Redisson 채택

### 채택 이유

1. **개발 생산성**
   - 복잡한 분산락 로직을 직접 구현할 필요 없음
   - 검증된 라이브러리로 안정성 보장
   - 빠른 개발 및 배포 가능

2. **운영 안정성**
   - 다양한 예외 상황에 대한 자동 처리
   - 락 해제 실패로 인한 시스템 장애 방지
   - 자동 락 갱신으로 긴 작업 시간에도 안정성 보장

3. **유지보수성**
   - 표준화된 API로 코드 가독성 향상
   - 버그 수정 및 기능 개선이 라이브러리 업데이트로 해결

4. **비즈니스 요구사항 부합**
   - 주문/결제 시스템의 높은 안정성 요구사항 충족
   - 동시성 문제로 인한 재고 오차, 중복 결제 등 방지
   - 24/7 운영 환경에서의 안정성 보장

### 구현 계획

1. **의존성 추가**
   ```gradle
   implementation("org.redisson:redisson-spring-boot-starter:3.27.0")
   ```

2. **설정 클래스 작성**
   ```java
   @Configuration
   public class RedissonConfig {
   
       @Value("${spring.data.redis.host:localhost}")
       private String redisHost;
   
       @Value("${spring.data.redis.port:6379}")
       private int redisPort;
   
       @Bean
       public RedissonClient redissonClient() {
           Config config = new Config();
           config.useSingleServer()
                   .setAddress("redis://" + redisHost + ":" + redisPort)
                   .setConnectionMinimumIdleSize(5)
                   .setConnectionPoolSize(20)
                   .setIdleConnectionTimeout(10000)
                   .setConnectTimeout(10000)
                   .setTimeout(3000);
           return Redisson.create(config);
       }
   }
   ```

3. **서비스 레이어 적용**
   - OrderService: 주문 생성 시 재고 락
   - PaymentService: 결제 처리 시 중복 결제 방지 락
   - PointService: 포인트 차감 시 동시성 제어

4. **모니터링 및 로깅**
   - 락 획득/해제 로그 추가
   - 락 대기 시간 모니터링
   - 데드락 상황 감지 및 알림

## 5. 주의사항

1. **락 키 설계**: 비즈니스 로직에 맞는 적절한 락 키 설계 필요
2. **락 유지 시간**: 작업 시간을 고려한 적절한 락 유지 시간 설정
3. **성능 영향**: 과도한 락 사용으로 인한 성능 저하 방지
4. **장애 대응**: Redis 장애 시 시스템 동작 방안 수립

---

**결론**: 개발 생산성, 운영 안정성, 유지보수성을 종합적으로 고려할 때 **Redisson**이 주문/결제 시스템의 분산락 구현에 최적의 선택이라고 판단해 채택했다.
