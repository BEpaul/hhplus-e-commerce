# Redis 기반 랭킹 시스템 & 선착순 쿠폰 발급 시스템 구현 회고

## 개요

이번에 Redis를 활용해서 랭킹 시스템과 선착순 쿠폰 발급 시스템을 구현했는데, 많은 것을 배울 수 있었습니다. 기존에 낙관적 락이나 분산락으로 해결하려던 동시성 문제를 Redis의 Sorted Set을 활용해서 훨씬 효율적으로 해결할 수 있었습니다.

## 랭킹 시스템 설계

### 기존 방식의 한계
처음에는 RDB 기반으로 랭킹을 계산하려고 했는데, 매번 전체 데이터를 조회해서 정렬하고 랭킹을 계산하는 방식이라 성능이 좋지 않았습니다. 특히 데이터가 많아지면 정말 느려질 것 같았습니다.

### Redis Sorted Set 도입
Redis의 Sorted Set을 활용해서 랭킹 시스템을 구현했는데, 이것이 정말 핵심이었습니다. 

- **점수 기반 정렬**: 상품의 판매량이나 조회수를 score로 사용해서 자동으로 정렬
- **실시간 랭킹**: `ZREVRANK` 명령어로 O(log N) 시간에 랭킹 조회 가능
- **메모리 기반**: 디스크 I/O 없이 빠른 응답

### 구현 포인트
```java
// 상품 조회 시 점수 증가
redisTemplate.opsForZSet().incrementScore(RANKING_KEY, productId.toString(), 1.0);

// 랭킹 조회
Long rank = redisTemplate.opsForZSet().reverseRank(RANKING_KEY, productId.toString());
```

이렇게 하니까 랭킹 조회가 정말 빨라졌습니다. RDB를 사용하는 것에 비해 밀리초 단위로 처리되니까 사용자 경험도 훨씬 좋아졌습니다.

## 선착순 쿠폰 발급 시스템 설계

### 기존 방식의 문제점
처음에는 낙관적 락으로 구현했는데, 동시 요청이 많아지면 실패율이 높아지는 문제가 있었습니다. 재시도 횟수가 늘어남에 따라 락 경합으로 인한 성능 저하가 걱정됐습니다.
분산락 또한 경합 걱정을 피할 수 없었습니다.

### Redis 기반 선착순 처리
마찬가지로 Redis의 Sorted Set을 활용해서 선착순 쿠폰 발급을 구현했습니다.

### 핵심 아이디어
1. **timestamp 기반 순서**: 사용자가 요청한 시간을 score로 사용
2. **순위 기반 제한**: 발급 순위가 쿠폰 수량 제한보다 작으면 발급 성공
3. **중복 발급 방지**: 사용자별 발급 상태를 Redis String으로 관리

```java
// 쿠폰 발급 시도
Long timestamp = System.currentTimeMillis();
redisTemplate.opsForZSet().add(queueKey, userId.toString(), timestamp);

// 순위 확인
Long userRank = redisTemplate.opsForZSet().rank(queueKey, userId.toString());
if (userRank >= couponLimit) {
    // 수량 초과 - 대기열에서 제거
    redisTemplate.opsForZSet().remove(queueKey, userId.toString());
    return false;
}
```

### 비동기 데이터 동기화
Redis에서 발급이 완료되면 즉시 응답하고, RDB 재고 차감은 Outbox Pattern으로 비동기 처리했습니다.

```java
// 즉시 응답 (Redis 기반)
boolean isIssued = couponRedisService.tryIssueCoupon(couponId, userId);

// 비동기 RDB 동기화
couponEventService.publishCouponIssuedEvent(couponId, userId, userCouponId);
```

## 기술적 도전과 해결

### 1. 동시성 테스트 문제
처음에 테스트가 계속 실패했는데, 기존 낙관적 락 방식에 맞춰진 테스트를 Redis 방식에 맞게 수정해야 했습니다. 에러 코드도 `OUT_OF_STOCK_COUPON`에서 `COUPON_ISSUANCE_FAILED`로 변경하고, 검증 로직도 Redis 기반으로 바꿨습니다.

### 2. 테스트 격리 문제
테스트 간에 Redis 데이터가 겹치면서 문제가 생겼는데, `@BeforeEach`에서 Redis 데이터를 정리하도록 했습니다. 특히 `couponRedisService.deleteCouponData()`로 쿠폰별 데이터를 깔끔하게 정리했습니다.
하지만 해당 방법이 임시방편이라고 생각해 테스트 환경을 완벽하게 격리하고 추후 언제든지 자유롭게 해당 환경을 사용할 수 있도록 세팅이 필요할 것 같습니다.

### 3. Outbox Pattern 구현
비동기 처리를 위한 Outbox Pattern을 처음 구현해봤는데, 이벤트를 DB에 저장하고 스케줄러가 주기적으로 처리하는 방식으로 데이터 정합성을 보장할 수 있었습니다.
결제 로직에서도 Outbox Pattern을 적용하여 리팩토링하면 좋을 것 같습니다.

## 성능 개선 효과

### 처리량 향상
- 기존: 초당 수백 건 처리
- 개선: 초당 수천 건 처리 (Redis 기반)

### 동시성 처리
- 기존: 낙관적 락으로 인한 높은 실패율
- 개선: Redis Sorted Set으로 정확한 선착순 처리

## 아키텍처 개선점

### 1. 확장성
Redis Cluster를 사용하면 수평 확장이 가능하고, 여러 서버에서도 일관된 랭킹과 쿠폰 발급이 가능합니다.

### 2. 장애 대응
Redis 장애 시에도 RDB 기반으로 fallback할 수 있도록 설계했고, Outbox Pattern으로 데이터 손실을 방지했습니다.

### 3. 모니터링
Redis 메트릭과 쿠폰 발급 통계를 모니터링해서 시스템 상태를 실시간으로 파악할 수 있습니다.

## 배운 점과 인사이트

### 1. Redis의 강력함
Sorted Set이라는 자료구조 하나로 랭킹과 선착순 처리를 모두 해결할 수 있다는 것이 놀라웠습니다. 기존에 복잡하게 생각했던 문제들이 Redis의 atomic operation을 통해 깔끔하게 해결되는 점이 인상적이었습니다. ㅎㅎ

### 2. 비동기 처리의 중요성
사용자 경험을 위해서는 빠른 응답이 중요한데, Outbox Pattern을 활용해서 데이터 정합성과 성능을 모두 확보할 수 있었습니다.

### 3. 테스트의 중요성
동시성 테스트를 통해 실제 운영 환경에서 발생할 수 있는 문제들을 미리 발견하고 해결할 수 있었습니다. 특히 Redis 기반 테스트는 기존과 다른 접근이 필요했는데, 이 과정에서 많이 배웠습니다.
<br>특히 처음에는 테스트가 Redis 운영DB를 건드려서 정합성에 문제가 생겼는데, 마찬가지로 testconainer를 활용해서 테스트 환경을 격리하고, Redis 데이터를 매번 초기화하는 방식으로 문제를 해결했습니다.

## 향후 개선 방향

### 1. 캐시 전략 고도화
Redis에 더 많은 데이터를 캐싱해서 RDB 부하를 줄이고, TTL 전략을 더 세밀하게 관리할 예정입니다.

### 2. 이벤트 기반 아키텍처 확장
Outbox Pattern을 다른 도메인에도 적용해서 전체적으로 이벤트 기반 아키텍처로 발전시킬 수 있을 것 같습니다.

### 3. 모니터링 강화
Redis 성능 지표와 쿠폰 발급 통계를 대시보드로 만들어서 실시간 모니터링을 강화할 계획입니다.

## 마무리

이번 프로젝트를 통해 Redis의 강력함을 제대로 체감할 수 있었습니다. 특히 Sorted Set을 활용한 랭킹 시스템과 선착순 처리 방식에서 많이 배웠고, 기존에 복잡하게 생각했던 동시성 문제들이 Redis의 atomic operation으로 깔끔하게 해결되니까 신기하면서도 뿌듯했습니다.

성능도 크게 개선되고, 사용자 경험도 좋아졌으니 이번 리팩토링이 정말 의미 있는 작업이었다고 생각합니다. 앞으로도 Redis를 활용한 다양한 최적화 기회들을 찾아보고 싶습니다!
