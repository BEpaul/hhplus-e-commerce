package kr.hhplus.be.server.application.bestseller;

import kr.hhplus.be.server.domain.bestseller.BestSeller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BestSellerCacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_KEY_PREFIX = "bestseller:";

    @SuppressWarnings("unchecked")
    public List<BestSeller> getCachedBestSellers(LocalDate date) {
        log.warn("캐시된 베스트셀러 조회 - 날짜: {}", CACHE_KEY_PREFIX + date);
        return (List<BestSeller>) redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + date);
    }

    public void cacheBestSellers(LocalDate date, List<BestSeller> bestSellers) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        long secondsUntilMidnight = Duration.between(now, midnight).getSeconds();
        log.warn("베스트셀러 캐싱 - 날짜: {}, 만료 시간(초): {}", CACHE_KEY_PREFIX + date, secondsUntilMidnight);
        redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + date, bestSellers, secondsUntilMidnight, TimeUnit.SECONDS);
    }
} 