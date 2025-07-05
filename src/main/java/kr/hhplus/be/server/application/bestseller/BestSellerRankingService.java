package kr.hhplus.be.server.application.bestseller;

import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.persistence.product.ProductRepository;
import kr.hhplus.be.server.interfaces.web.product.dto.response.ProductRankingInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.ZSetOperations;

@Slf4j
@Service
@RequiredArgsConstructor
public class BestSellerRankingService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;
    
    private static final String RANKING_KEY_PREFIX = "ranking:daily:";
    private static final String PRODUCT_PREFIX = "product:";
    private static final int TTL_DAYS = 31;
    
    /**
     * 새로운 날짜의 랭킹 키를 생성하고 모든 상품을 0점으로 초기화
     */
    public void initializeDailyRanking(LocalDate date) {
        String rankingKey = getRankingKey(date);
        
        log.info("새로운 날짜 랭킹 초기화 시작 - 키: {}", rankingKey);
        
        // 모든 상품 ID 조회
        List<Long> productIds = productRepository.findAll().stream()
                .map(Product::getId)
                .toList();
        
        if (productIds.isEmpty()) {
            log.warn("초기화할 상품이 없습니다.");
            return;
        }

        // Redis Sorted Set에 모든 상품을 0점으로 추가
        for (Long productId : productIds) {
            String member = PRODUCT_PREFIX + productId;
            redisTemplate.opsForZSet().add(rankingKey, member, 0);
        }
        
        // TTL 설정 (31일)
        redisTemplate.expire(rankingKey, TTL_DAYS, TimeUnit.DAYS);
        
        log.info("새로운 날짜 랭킹 초기화 완료 - 키: {}, 상품 수: {}", rankingKey, productIds.size());
    }

    /**
     * 오늘 날짜의 베스트셀러 TOP5 상품 정보 조회
     */
    public List<ProductRankingInfo> getTopProductsWithRanking() {
        String rankingKey = getTodayRankingKey();
        
        try {
            Set<ZSetOperations.TypedTuple<Object>> topProducts = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(rankingKey, 0, 4);
            
            if (topProducts == null || topProducts.isEmpty()) {
                log.warn("베스트셀러 데이터가 없습니다 - 키: {}", rankingKey);
                return List.of();
            }
            
            List<ProductRankingInfo> rankingInfos = topProducts.stream()
                    .map(tuple -> {
                        String productStr = Objects.requireNonNull(tuple.getValue()).toString();
                        Long productId = Long.valueOf(productStr.replace(PRODUCT_PREFIX, ""));
                        Double sales = tuple.getScore();
                        
                        // reverseRank를 사용하여 실제 랭킹 계산 (0부터 시작)
                        Long ranking = redisTemplate.opsForZSet().reverseRank(rankingKey, productStr);
                        if (ranking == null) {
                            ranking = 0L;
                        }
                        
                        return new ProductRankingInfo(productId, sales != null ? sales.longValue() : 0L, ranking + 1);
                    })
                    .toList();
            
            log.debug("베스트셀러 TOP5 조회 (판매량 및 랭킹 포함) - 키: {}", rankingKey);
            return rankingInfos;
        } catch (Exception e) {
            log.error("Redis 베스트셀러 조회 실패 - 키: {}", rankingKey, e);
            return List.of();
        }
    }

    /**
     * 오늘 날짜의 상품 판매량 증가
     */
    public void incrementTodaySales(Long productId, Long quantity) {
        incrementSalesCount(LocalDate.now(), productId, quantity);
        log.info("오늘 판매량 증가 - 상품 ID: {}, 수량: {}", productId, quantity);
    }

    /**
     * 상품 판매량 증가
     */
    public void incrementSalesCount(LocalDate date, Long productId, Long quantity) {
        String rankingKey = getRankingKey(date);
        String member = PRODUCT_PREFIX + productId;

        Double newScore = redisTemplate.opsForZSet().incrementScore(rankingKey, member, quantity);
        log.debug("판매량 증가 - 키: {}, 상품: {}, 수량: {}, 새로운 점수: {}",
                rankingKey, member, quantity, newScore);
    }

    /**
     * 특정 날짜의 모든 상품 판매량 조회
     */
    public Set<ZSetOperations.TypedTuple<Object>> getAllProductSales(LocalDate date) {
        String rankingKey = getRankingKey(date);
        
        Set<ZSetOperations.TypedTuple<Object>> allProducts = redisTemplate.opsForZSet()
                .rangeWithScores(rankingKey, 0, -1);

        if (allProducts != null) {
            log.debug("전체 상품 판매량 조회 - 키: {}, 결과 수: {}", rankingKey, allProducts.size());
        }
        return allProducts;
    }

    /**
     * 오늘 날짜의 랭킹 키 생성
     */
    private String getTodayRankingKey() {
        return getRankingKey(LocalDate.now());
    }

    /**
     * 랭킹 키 생성
     */
    private String getRankingKey(LocalDate date) {
        return RANKING_KEY_PREFIX + date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
} 